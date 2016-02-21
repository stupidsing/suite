package suite.file.impl;

import java.io.Closeable;
import java.io.FileNotFoundException;
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

	public static class RandomAccessFileException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public RandomAccessFileException(IOException ex) {
			super(ex);
		}
	}

	public RandomAccessibleFile(String filename) {
		FileUtil.mkdir(Paths.get(filename).getParent());
		try {
			file = new RandomAccessFile(filename, "rw");
		} catch (FileNotFoundException ex) {
			throw new RandomAccessFileException(ex);
		}
		channel = file.getChannel();
	}

	@Override
	public void close() {
		try {
			channel.close();
			file.close();
		} catch (IOException ex) {
			throw new RandomAccessFileException(ex);
		}
	}

	public void sync() {
		try {
			channel.force(true);
		} catch (IOException ex) {
			throw new RandomAccessFileException(ex);
		}
	}

	public Bytes load(int start, int end) {
		int size = end - start;
		ByteBuffer bb = ByteBuffer.allocate(size);

		try {
			channel.read(bb, start);
		} catch (IOException ex) {
			throw new RandomAccessFileException(ex);
		}

		bb.limit(size);
		return Bytes.of(bb);
	}

	public void save(int start, Bytes bytes) {
		try {
			channel.write(bytes.toByteBuffer(), start);
		} catch (IOException ex) {
			throw new RandomAccessFileException(ex);
		}
	}

}
