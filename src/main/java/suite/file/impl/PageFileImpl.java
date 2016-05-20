package suite.file.impl;

import java.nio.file.Path;

import suite.file.PageFile;
import suite.primitive.Bytes;
import suite.util.Util;

public class PageFileImpl implements PageFile {

	private RandomAccessibleFile file;
	private int pageSize;

	public PageFileImpl(Path path, int pageSize) {
		file = new RandomAccessibleFile(path);
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
	public Bytes load(Integer pointer) {
		int start = pointer * pageSize, end = start + pageSize;
		return file.load(start, end);
	}

	@Override
	public void save(Integer pointer, Bytes bytes) {
		Util.assert_(bytes.size() <= pageSize);
		file.save(pointer * pageSize, bytes);
	}

}
