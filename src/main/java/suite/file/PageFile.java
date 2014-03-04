package suite.file;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PageFile implements Closeable {

	private RandomAccessibleFile file;
	private int pageSize;

	public PageFile(String filename, int pageSize) throws FileNotFoundException {
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

	public ByteBuffer load(int pageNo) throws IOException {
		int start = pageNo * pageSize, end = start + pageSize;
		return file.load(start, end);
	}

	public void save(int pageNo, ByteBuffer buffer) throws IOException {
		file.save(pageNo * pageSize, buffer);
	}

	public int getPageSize() {
		return pageSize;
	}

}
