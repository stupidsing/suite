package suite.file;

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
 * pageSize >= sizeof(char) + 2 * sizeof(int) + branchFactor * branchPointerSize
 * 
 * where branchPointerSize = max(sizeof(int), sizeof(Value))
 */
public class PageFile<V> implements Closeable {

	private static final int pageSize = 4096;

	private RandomAccessFile file;
	private FileChannel channel;
	private Serializer<V> serializer;

	public PageFile(String filename, Serializer<V> serializer) throws FileNotFoundException {
		file = new RandomAccessFile(filename, "rw");
		channel = file.getChannel();
		this.serializer = serializer;
	}

	@Override
	public void close() throws IOException {
		channel.close();
		file.close();
	}

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
