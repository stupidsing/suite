package org.btree;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Persists B-tree pages to file on disk.
 * 
 * The following must holds when using this class:
 * 
 * pageSize >= sizeof(char) + sizeof(int) + branchFactor * sizeof(int)
 * 
 * pageSize >= sizeof(char) + sizeof(int) + leafFactor * sizeof(Value)
 */
public class FilePersister<V> implements Persister<V>, Closeable {

	private static final int pageSize = 4096;

	private RandomAccessFile file;
	private FileChannel channel;
	private Serializer<V> serializer;

	public FilePersister(String filename, Serializer<V> serializer)
			throws FileNotFoundException {
		file = new RandomAccessFile(filename, "rw");
		channel = file.getChannel();
		this.serializer = serializer;
	}

	@Override
	public void close() throws IOException {
		channel.close();
		file.close();
	}

	@Override
	public V load(int pageNo) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			channel.read(buffer, pageNo * pageSize);
			buffer.rewind();
			return serializer.read(buffer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void save(int pageNo, V page) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			serializer.write(buffer, page);
			buffer.flip();
			channel.write(buffer, pageNo * pageSize);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
