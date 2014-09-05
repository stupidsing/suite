package suite.file;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import suite.primitive.Bytes;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(String filename) throws FileNotFoundException {
		file = new RandomAccessFile(filename, "rw");
		channel = file.getChannel();
	}

	@Override
	public void close() throws IOException {
		channel.close();
		file.close();
	}

	public void sync() throws IOException {
		channel.force(true);
	}

	public Bytes load(int start, int end) throws IOException {
		int size = end - start;

		ByteBuffer bb = ByteBuffer.allocate(size);
		channel.read(bb, start);
		bb.limit(size);
		return Bytes.of(bb);
	}

	public void save(int start, Bytes bytes) throws IOException {
		channel.write(bytes.toByteBuffer(), start);
	}

}
