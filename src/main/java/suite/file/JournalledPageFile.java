package suite.file;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
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

	private Serializer<Bytes> bytesSerializer;

	private Serializer<JournalEntry> jes = new Serializer<JournalEntry>() {
		public JournalEntry read(DataInput dataInput) throws IOException {
			int pageNo = dataInput.readInt();
			return new JournalEntry(pageNo, bytesSerializer.read(dataInput));
		}

		public void write(DataOutput dataOutput, JournalEntry journalEntry) throws IOException {
			dataOutput.writeInt(journalEntry.pageNo);
			bytesSerializer.write(dataOutput, journalEntry.bytes);
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
		int journalPageSize = pageSize + 4;
		journalPageFile = new SerializedPageFile<>(new PageFileImpl(filename + ".journal", journalPageSize), jes);
		pointerPageFile = new SerializedPageFile<>(new PageFileImpl(filename, 4), SerializeUtil.intSerializer);
		bytesSerializer = SerializeUtil.bytes(pageSize);

		nCommittedJournalEntries = pointerPageFile.load(0);

		for (int jp = 0; jp < nCommittedJournalEntries; jp++)
			journalEntries.add(journalPageFile.load(jp));
	}

	public synchronized void create() {
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

	public synchronized void flushJournal() throws IOException {

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
	public synchronized Bytes load(int pageNo) throws IOException {
		int jp = findPageInJournal(pageNo);
		if (jp < 0)
			return pageFile.load(pageNo);
		else
			return journalEntries.get(jp).bytes;
	}

	@Override
	public synchronized void save(int pageNo, Bytes bytes) throws IOException {
		int jp = findDirtyPageInJournal(pageNo);

		if (jp < 0) {
			jp = journalEntries.size();
			journalEntries.add(new JournalEntry(pageNo, null));
		}

		JournalEntry journalEntry = journalEntries.get(jp);
		journalEntry.bytes = bytes;
		journalPageFile.save(jp, journalEntry);

		pageFile.save(pageNo, bytes);
	}

	public synchronized void commit() {
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
