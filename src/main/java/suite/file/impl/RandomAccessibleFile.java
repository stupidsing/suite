package suite.file.impl;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import suite.os.FileUtil;
import suite.primitive.Bytes;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(Path path) {
		FileUtil.mkdir(path.getParent());
		try {
			file = new RandomAccessFile(path.toFile(), "rw");
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		channel = file.getChannel();
	}

	@Override
	public void close() {
		try {
			channel.close();
			file.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void sync() {
		try {
			channel.force(true);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Bytes load(int start, int end) {
		int size = end - start;
		ByteBuffer bb = ByteBuffer.allocate(size);

		try {
			channel.read(bb, start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		bb.limit(size);
		return Bytes.of(bb);
	}

	public void save(int start, Bytes bytes) {
		try {
			channel.write(bytes.toByteBuffer(), start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
