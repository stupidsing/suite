package suite.file.impl;

import java.io.Closeable;

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
	public void sync() {
		parent.sync();
	}

	@Override
	public Bytes load(Integer pointer) {
		return parent.load(convert(pointer));
	}

	@Override
	public void save(Integer pointer, Bytes bytes) {
		parent.save(convert(pointer), bytes);
	}

	@Override
	public void close() {
	}

	private Integer convert(Integer pointer0) {
		int pointer1 = pointer0 + startPointer;

		if (startPointer <= pointer1 && pointer1 < endPointer)
			return pointer1;
		else
			throw new RuntimeException("Page index out of range");
	}

}
