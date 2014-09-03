package suite.file;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import suite.primitive.Bytes;

public class BytesPageFile implements Closeable {

	private RandomAccessibleFile file;
	private int pageSize;

	public BytesPageFile(String filename, int pageSize) throws FileNotFoundException {
		file = new RandomAccessibleFile(filename);
		this.pageSize = pageSize;
	}

	@Override
	public void close() throws IOException {
		file.close();
	}

	public void sync() throws IOException {
		file.sync();
	}

	public Bytes load(int pageNo) throws IOException {
		int start = pageNo * pageSize, end = start + pageSize;
		ByteBuffer bb = file.load(start, end);
		int offset = bb.arrayOffset();
		return new Bytes(bb.array(), offset, offset + bb.limit());
	}

	public void save(int pageNo, Bytes buffer) throws IOException {
		file.save(pageNo * pageSize, buffer.toByteBuffer());
	}

	public int getPageSize() {
		return pageSize;
	}

}
