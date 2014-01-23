package suite.file;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class AccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public AccessibleFile(String filename) throws FileNotFoundException {
		file = new RandomAccessFile(filename, "rw");
		channel = file.getChannel();
	}

	@Override
	public void close() throws IOException {
		channel.close();
		file.close();
	}

	public ByteBuffer load(int start, int end) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(end - start);
		channel.read(buffer, start);
		buffer.rewind();
		return buffer;
	}

	public void save(int start, ByteBuffer buffer) throws IOException {
		buffer.flip();
		channel.write(buffer, start);
	}

}
