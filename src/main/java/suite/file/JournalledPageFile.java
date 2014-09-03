package suite.file;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import suite.primitive.Bytes;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class JournalledPageFile implements Closeable, PageFile {

	private PageFile pageFile;
	private SerializedPageFile<JournalEntry> journalPageFile;
	private SerializedPageFile<Integer> pointerPageFile;

	private int nCommittedJournalEntries;
	private List<JournalEntry> journalEntries = new ArrayList<>();

	private Serializer<JournalEntry> jes = new Serializer<JournalEntry>() {
		public JournalEntry read(ByteBuffer buffer) {
			int pageNo = buffer.getInt();
			return new JournalEntry(pageNo, new Bytes(buffer));
		}

		public void write(ByteBuffer buffer, JournalEntry journalEntry) {
			buffer.putInt(journalEntry.pageNo);
			journalEntry.bytes.putByteBuffer(buffer);
		}
	};

	private class JournalEntry {
		private int pageNo;
		private Bytes bytes;

		private JournalEntry(int pageNo, Bytes bytes) {
			this.pageNo = pageNo;
			this.bytes = bytes;
		}
	}

	public JournalledPageFile(String filename, int pageSize) throws IOException {
		pageFile = new PageFileImpl(filename, pageSize);
		journalPageFile = new SerializedPageFile<>(filename + ".journal", jes);
		pointerPageFile = new SerializedPageFile<>(new PageFileImpl(filename, 4), SerializeUtil.intSerializer);

		nCommittedJournalEntries = pointerPageFile.load(0);

		for (int jp = 0; jp < nCommittedJournalEntries; jp++)
			journalEntries.add(journalPageFile.load(jp));
	}

	public void create() {
		nCommittedJournalEntries = 0;
		journalEntries.clear();
		pointerPageFile.save(0, nCommittedJournalEntries);
	}

	@Override
	public void close() throws IOException {
		pageFile.close();
		journalPageFile.close();
		pointerPageFile.close();
	}

	@Override
	public void sync() throws IOException {
		journalPageFile.sync();
		pointerPageFile.save(0, nCommittedJournalEntries);

		if (nCommittedJournalEntries > 64)
			flushJournal();
	}

	public void flushJournal() throws IOException {

		// Make sure all changes are written to main file
		pageFile.sync();

		// Clear all committed entries
		journalEntries.subList(0, nCommittedJournalEntries).clear();

		// Reset committed pointer
		nCommittedJournalEntries = 0;
		pointerPageFile.save(0, nCommittedJournalEntries);
		pointerPageFile.sync();

		// Write back entries for next commit
		for (int jp = 0; jp < journalEntries.size(); jp++)
			journalPageFile.save(jp, journalEntries.get(jp));
	}

	@Override
	public ByteBuffer load(int pageNo) throws IOException {
		int jp = findPageInJournal(pageNo);
		if (jp >= 0)
			return pageFile.load(pageNo);
		else
			return journalEntries.get(jp).bytes.toByteBuffer();
	}

	@Override
	public void save(int pageNo, ByteBuffer bb) throws IOException {
		int jp = findDirtyPageInJournal(pageNo);

		if (jp < 0) {
			jp = journalEntries.size();
			journalEntries.add(new JournalEntry(pageNo, null));
		}

		JournalEntry journalEntry = journalEntries.get(jp);
		journalEntry.bytes = new Bytes(bb);
		journalPageFile.save(jp, journalEntry);

		pageFile.save(pageNo, bb);
	}

	public void commit() throws IOException {
		nCommittedJournalEntries = journalEntries.size();
	}

	private int findPageInJournal(int pageNo) {
		return findPageInJournal(pageNo, 0);
	}

	private int findDirtyPageInJournal(int pageNo) {
		return findPageInJournal(pageNo, nCommittedJournalEntries);
	}

	private int findPageInJournal(int pageNo, int start) {
		int jp1 = -1;
		for (int jp = start; jp < journalEntries.size(); jp++)
			if (journalEntries.get(jp).pageNo == pageNo)
				jp1 = jp;
		return jp1;
	}

}
