package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.PageFile;
import suite.primitive.Bytes;

public class PageFileImpl implements Closeable, PageFile {

	private RandomAccessibleFile file;
	private int pageSize;

	public PageFileImpl(String filename, int pageSize) throws IOException {
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
	public Bytes load(Integer pointer) throws IOException {
		int start = pointer * pageSize, end = start + pageSize;
		return file.load(start, end);
	}

	@Override
	public void save(Integer pointer, Bytes bytes) throws IOException {
		file.save(pointer * pageSize, bytes);
	}

}
