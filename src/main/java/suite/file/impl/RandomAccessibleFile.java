package suite.file.impl;

import primal.Verbs.Close;
import primal.Verbs.Mk;
import primal.primitive.adt.Bytes;

import java.io.Closeable;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static primal.statics.Rethrow.ex;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(Path path) {
		Mk.dir(path.getParent());
		file = ex(() -> new RandomAccessFile(path.toFile(), "rw"));
		channel = file.getChannel();
	}

	@Override
	public void close() {
		Close.quietly(channel, file);
	}

	public void sync() {
		ex(() -> {
			channel.force(true);
			return channel;
		});
	}

	public Bytes load(int start, int end) {
		return ex(() -> {
			var size = end - start;
			var bb = ByteBuffer.allocate(size);
			channel.read(bb, start);
			bb.limit(size);
			return Bytes.of(bb);
		});
	}

	public void save(int start, Bytes bytes) {
		ex(() -> {
			channel.write(bytes.toByteBuffer(), start);
			return channel;
		});
	}

}
