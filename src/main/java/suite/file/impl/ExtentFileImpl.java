package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.primitive.Bytes;

public class ExtentFileImpl implements Closeable, DataFile<Extent> {

	private RandomAccessibleFile file;
	private int pageSize;

	public ExtentFileImpl(String filename, int pageSize) throws IOException {
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
	public Bytes load(Extent extent) throws IOException {
		int start = extent.start * pageSize, end = start + extent.count * pageSize;
		return file.load(start, end);
	}

	@Override
	public void save(Extent extent, Bytes bytes) throws IOException {
		file.save(extent.start * pageSize, bytes);
	}

	public int getPageSize() {
		return pageSize;
	}

}
