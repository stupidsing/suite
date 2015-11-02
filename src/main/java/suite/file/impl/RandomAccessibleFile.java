package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

import suite.os.FileUtil;
import suite.primitive.Bytes;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(String filename) throws IOException {
		FileUtil.mkdir(Paths.get(filename).getParent());
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
		bb.limit(size);
		return Bytes.of(bb);
	}

	public void save(int start, Bytes bytes) throws IOException {
		channel.write(bytes.toByteBuffer(), start);
	}

}
