package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.PageFile;
import suite.primitive.Bytes;

public class SubPageFileImpl implements Closeable, PageFile {

	private PageFile parent;
	private int startPage, endPage;

	public SubPageFileImpl(PageFile parent, int startPage, int endPage) {
		this.parent = parent;
		this.startPage = startPage;
		this.endPage = endPage;
	}

	@Override
	public void sync() throws IOException {
		parent.sync();
	}

	@Override
	public Bytes load(Integer pageNo) throws IOException {
		return parent.load(validate(pageNo + startPage));
	}

	@Override
	public void save(Integer pageNo, Bytes bytes) throws IOException {
		parent.save(validate(pageNo + startPage), bytes);
	}

	@Override
	public void close() throws IOException {
	}

	private int validate(int index) throws IOException {
		if (startPage <= index && index < endPage)
			return index;
		else
			throw new IOException("Page index out of range");
	}

}
