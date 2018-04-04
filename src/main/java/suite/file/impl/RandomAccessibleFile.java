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
import suite.util.Fail;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(Path path) {
		FileUtil.mkdir(path.getParent());
		try {
			file = new RandomAccessFile(path.toFile(), "rw");
		} catch (FileNotFoundException ex) {
			Fail.t(ex);
		}
		channel = file.getChannel();
	}

	@Override
	public void close() {
		try {
			channel.close();
			file.close();
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	public void sync() {
		try {
			channel.force(true);
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	public Bytes load(int start, int end) {
		var size = end - start;
		ByteBuffer bb = ByteBuffer.allocate(size);

		try {
			channel.read(bb, start);
		} catch (IOException ex) {
			Fail.t(ex);
		}

		bb.limit(size);
		return Bytes.of(bb);
	}

	public void save(int start, Bytes bytes) {
		try {
			channel.write(bytes.toByteBuffer(), start);
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

}
