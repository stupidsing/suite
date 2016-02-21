package suite.file.impl;

import java.io.Closeable;

import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.primitive.Bytes;

public class ExtentFileImpl implements Closeable, ExtentFile {

	private RandomAccessibleFile file;
	private int pageSize;

	public ExtentFileImpl(String filename, int pageSize) {
		file = new RandomAccessibleFile(filename);
		this.pageSize = pageSize;
	}

	@Override
	public void close() {
		file.close();
	}

	@Override
	public void sync() {
		file.sync();
	}

	@Override
	public Bytes load(Extent extent) {
		int start = extent.start * pageSize, end = extent.end * pageSize;
		return file.load(start, end);
	}

	@Override
	public void save(Extent extent, Bytes bytes) {
		file.save(extent.start * pageSize, bytes);
	}

	public int getPageSize() {
		return pageSize;
	}

}
