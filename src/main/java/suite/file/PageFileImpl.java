package suite.file;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PageFileImpl implements Closeable, PageFile {

	private RandomAccessibleFile file;
	private int pageSize;

	public PageFileImpl(String filename, int pageSize) throws FileNotFoundException {
		file = new RandomAccessibleFile(filename);
		this.pageSize = pageSize;
	}

	@Override
	public void close() throws IOException {
		file.close();
	}

	@Override
	public void sync() throws IOException {
		file.sync();
	}

	@Override
	public ByteBuffer load(int pageNo) throws IOException {
		int start = pageNo * pageSize, end = start + pageSize;
		return file.load(start, end);
	}

	@Override
	public void save(int pageNo, ByteBuffer buffer) throws IOException {
		file.save(pageNo * pageSize, buffer);
	}

	public int getPageSize() {
		return pageSize;
	}

}
