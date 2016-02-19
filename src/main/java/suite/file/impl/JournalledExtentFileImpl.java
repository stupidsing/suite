package suite.file.impl;

import java.io.IOException;

import suite.file.ExtentAllocator.Extent;
import suite.file.JournalledExtentFile;
import suite.file.JournalledPageFile;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

public class JournalledExtentFileImpl implements JournalledExtentFile {

	private JournalledPageFile journalledPageFile;
	private int pageSize;

	public JournalledExtentFileImpl(String filename, int pageSize) throws IOException {
		this.journalledPageFile = new JournalledPageFileImpl(filename, pageSize);
		this.pageSize = pageSize;
	}

	@Override
	public void close() throws IOException {
		journalledPageFile.close();
	}

	@Override
	public void sync() throws IOException {
		journalledPageFile.sync();
	}

	@Override
	public Bytes load(Extent extent) throws IOException {
		BytesBuilder bb = new BytesBuilder();
		for (int pointer = extent.start; pointer < extent.start + extent.count; pointer++)
			bb.append(journalledPageFile.load(pointer));
		return bb.toBytes();
	}

	@Override
	public void save(Extent extent, Bytes bytes) throws IOException {
		int offset = 0;
		for (int pointer = extent.start; pointer < extent.start + extent.count; pointer++) {
			int offset0 = offset;
			journalledPageFile.save(pointer, bytes.subbytes(offset0, offset += pageSize));
		}
	}

	@Override
	public void commit() throws IOException {
		journalledPageFile.commit();
	}

}
