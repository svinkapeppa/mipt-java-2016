package ru.mipt.java2016.homework.g594.shevkunov.task3;

import ru.mipt.java2016.homework.g594.shevkunov.task2.LazyMergedKeyValueStorageSerializator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

/**
 * Created by shevkunov on 14.11.16.
 */
public class LazyMergedKeyValueStorageKeeper<K, V> {
    private final String fileNamePrefix;
    private final String fileNameSuffix;
    private final Vector<RandomAccessFile> dataFiles = new Vector<>();
    private final LazyMergedKeyValueStorageSerializator<V> valueSerializator;

    public LazyMergedKeyValueStorageKeeper(LazyMergedKeyValueStorageSerializator<K> keySerializator, // TODO Is needed?
                                           LazyMergedKeyValueStorageSerializator<V> valueSerializator,
                                           String fileNamePrefix, String fileNameSuffix,
                                           int dataFilesCount, boolean createNewFiles) throws IOException {
        this.fileNamePrefix = fileNamePrefix;
        this.fileNameSuffix = fileNameSuffix;
        this.valueSerializator = valueSerializator;
        grow(dataFilesCount, createNewFiles);
    }

    private void grow(int newDataFilesCount, boolean createNewFiles) throws IOException {
        int oldSize = dataFiles.size();
        if (newDataFilesCount < 0) {
            throw new RuntimeException("Can only grow!");
        }
        dataFiles.setSize(oldSize + newDataFilesCount);
        for (int i = oldSize; i < dataFiles.size(); ++i) {
            File tryDataFile = new File(getName(i));
            if (createNewFiles) {
                if (tryDataFile.exists()) {
                    throw new RuntimeException("Data files already exists");
                } else {
                    tryDataFile.createNewFile();
                }
            } else {
                if (!tryDataFile.exists()) {
                    throw new RuntimeException("Broken data of header files");
                }
            }
            dataFiles.set(i, new RandomAccessFile(tryDataFile, "rw"));

        }
    }

    private String getName(int index) {
        return fileNamePrefix + Integer.toString(index) + fileNameSuffix;
    }

    public LazyMergedKeyValueStorageFileNode write(int fileIndex, V value) throws IOException {
        return new LazyMergedKeyValueStorageFileNode(fileIndex, writeToFile(dataFiles.get(fileIndex), value));
    }

    public V read(LazyMergedKeyValueStorageFileNode pointer) throws IOException {
        return loadFromFile(dataFiles.get((int) pointer.getFile()), pointer.getOffset());
    }

    private long writeToFile(RandomAccessFile out, V value) throws IOException {
        byte[] bytes = valueSerializator.serialize(value);
        byte[] sizeBytes  = valueSerializator.toBytes(bytes.length);
        long endOffset = out.length();
        out.seek(endOffset);
        out.write(sizeBytes);
        out.write(bytes);
        return endOffset;
    }

    private V loadFromFile(RandomAccessFile in, long seek) throws IOException {
        byte[] sizeBytes = new byte[8];
        in.seek(seek);
        in.read(sizeBytes);
        long size = valueSerializator.toLong(sizeBytes);
        byte[] bytes = new byte[(int) size];
        in.read(bytes);
        return valueSerializator.deSerialize(bytes);
    }

    public int newFile() throws IOException {
        grow(1, true);
        return dataFiles.size() - 1;
    }

    public void swap(int a, int b) throws IOException {
        dataFiles.get(a).close();
        dataFiles.get(b).close();

        File aFile = new File(getName(a));
        File bFile = new File(getName(b));
        File temp = new File(getName(-1));

        aFile.renameTo(temp);
        bFile.renameTo(aFile);
        temp.renameTo(bFile);

        dataFiles.set(a, new RandomAccessFile(aFile, "rw"));
        dataFiles.set(b, new RandomAccessFile(bFile, "rw"));
    }

    public void popBack() {
        if (dataFiles.isEmpty()) {
            throw new RuntimeException("Trying to delete unknown file");
        }
        File del = new File(getName(dataFiles.size() - 1));
        del.delete();
        dataFiles.setSize(dataFiles.size() - 1);
    }

    public void close() throws IOException {
        for (RandomAccessFile f : dataFiles) {
            f.close();
        }
    }
}
