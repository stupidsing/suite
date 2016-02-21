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

	public JournalledExtentFileImpl(String filename, int pageSize) {
		this.journalledPageFile = new JournalledPageFileImpl(filename, pageSize);
		this.pageSize = pageSize;
	}

	@Override
	public void close() throws IOException {
		journalledPageFile.close();
	}

	@Override
	public void sync() {
		journalledPageFile.sync();
	}

	@Override
	public Bytes load(Extent extent) {
		BytesBuilder bb = new BytesBuilder();
		for (int pointer = extent.start; pointer < extent.end; pointer++)
			bb.append(journalledPageFile.load(pointer));
		return bb.toBytes();
	}

	@Override
	public void save(Extent extent, Bytes bytes) {
		int offset = 0;
		for (int pointer = extent.start; pointer < extent.end; pointer++) {
			int offset0 = offset;
			journalledPageFile.save(pointer, bytes.subbytes(offset0, offset += pageSize));
		}
	}

	@Override
	public void commit() {
		journalledPageFile.commit();
	}

}
