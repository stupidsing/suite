package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.primitive.Bytes;

public class SubExtentFileImpl implements Closeable, ExtentFile {

	private DataFile<Extent> parent;
	private int startPointer, endPointer;

	public SubExtentFileImpl(DataFile<Extent> parent, int startPointer, int endPointer) {
		this.parent = parent;
		this.startPointer = startPointer;
		this.endPointer = endPointer;
	}

	@Override
	public void sync() throws IOException {
		parent.sync();
	}

	@Override
	public Bytes load(Extent extent) throws IOException {
		return parent.load(convert(extent));
	}

	@Override
	public void save(Extent extent, Bytes bytes) throws IOException {
		parent.save(convert(extent), bytes);
	}

	@Override
	public void close() throws IOException {
	}

	private Extent convert(Extent extent0) throws IOException {
		Extent extent1 = new Extent(extent0.start + startPointer, extent0.end + startPointer);

		if (startPointer <= extent1.start && extent1.end <= endPointer)
			return extent1;
		else
			throw new IOException("Extent index out of range");
	}

}
