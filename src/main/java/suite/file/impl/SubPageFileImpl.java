package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.PageFile;
import suite.primitive.Bytes;

public class SubPageFileImpl implements Closeable, PageFile {

	private PageFile parent;
	private int startPointer, endPointer;

	public SubPageFileImpl(PageFile parent, int startPointer, int endPointer) {
		this.parent = parent;
		this.startPointer = startPointer;
		this.endPointer = endPointer;
	}

	@Override
	public void sync() throws IOException {
		parent.sync();
	}

	@Override
	public Bytes load(Integer pointer) throws IOException {
		return parent.load(convert(pointer));
	}

	@Override
	public void save(Integer pointer, Bytes bytes) throws IOException {
		parent.save(convert(pointer), bytes);
	}

	@Override
	public void close() throws IOException {
	}

	private Integer convert(Integer pointer0) throws IOException {
		int pointer1 = pointer0 + startPointer;

		if (startPointer <= pointer1 && pointer1 < endPointer)
			return pointer1;
		else
			throw new IOException("Page index out of range");
	}

}
