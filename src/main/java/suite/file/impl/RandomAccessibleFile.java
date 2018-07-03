package suite.file.impl;

import java.io.Closeable;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.util.Object_;
import suite.util.Rethrow;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(Path path) {
		FileUtil.mkdir(path.getParent());
		file = Rethrow.ex(() -> new RandomAccessFile(path.toFile(), "rw"));
		channel = file.getChannel();
	}

	@Override
	public void close() {
		Object_.closeQuietly(channel, file);
	}

	public void sync() {
		Rethrow.ex(() -> {
			channel.force(true);
			return channel;
		});
	}

	public Bytes load(int start, int end) {
		return Rethrow.ex(() -> {
			var size = end - start;
			var bb = ByteBuffer.allocate(size);
			channel.read(bb, start);
			bb.limit(size);
			return Bytes.of(bb);
		});
	}

	public void save(int start, Bytes bytes) {
		Rethrow.ex(() -> {
			channel.write(bytes.toByteBuffer(), start);
			return channel;
		});
	}

}
