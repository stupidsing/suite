package suite.file.impl;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import suite.file.PageFile;
import suite.primitive.Bytes;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

/**
 * Protect data against power loss failures by recording journals.
 *
 * @author ywsing
 */
public class JournalledPageFileImpl implements Closeable, PageFile {

	private PageFile pageFile;
	private SerializedPageFile<JournalEntry> journalPageFile;
	private SerializedPageFile<Integer> pointerPageFile;

	private int nCommittedJournalEntries;
	private List<JournalEntry> journalEntries = new ArrayList<>();

	private Serializer<Bytes> bytesSerializer;

	private Serializer<JournalEntry> jes = new Serializer<JournalEntry>() {
		public JournalEntry read(DataInput dataInput) throws IOException {
			int pointer = dataInput.readInt();
			return new JournalEntry(pointer, bytesSerializer.read(dataInput));
		}

		public void write(DataOutput dataOutput, JournalEntry journalEntry) throws IOException {
			dataOutput.writeInt(journalEntry.pointer);
			bytesSerializer.write(dataOutput, journalEntry.bytes);
		}
	};

	private class JournalEntry {
		private int pointer;
		private Bytes bytes;

		private JournalEntry(int pointer, Bytes bytes) {
			this.pointer = pointer;
			this.bytes = bytes;
		}
	}

	public JournalledPageFileImpl(String filename, int pageSize) throws IOException {
		int journalPageSize = pageSize + 4;
		pageFile = new PageFileImpl(filename, pageSize);
		journalPageFile = new SerializedPageFile<>(new PageFileImpl(filename + ".journal", journalPageSize), jes);
		pointerPageFile = new SerializedPageFile<>(new PageFileImpl(filename + ".pointer", 4), SerializeUtil.intSerializer);
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

	/**
	 * Marks a snapshot that data can be recovered to.
	 */
	public synchronized void commit() throws IOException {
		while (nCommittedJournalEntries < journalEntries.size()) {
			JournalEntry journalEntry = journalEntries.get(nCommittedJournalEntries++);
			pageFile.save(journalEntry.pointer, journalEntry.bytes);
		}

		if (nCommittedJournalEntries > 8)
			saveJournal();
	}

	/**
	 * Makes sure the current snapshot of data is saved and recoverable on
	 * failure, upon the return of method call.
	 */
	@Override
	public synchronized void sync() throws IOException {
		journalPageFile.sync();
		saveJournal();
		pointerPageFile.sync();
	}

	private void saveJournal() throws IOException {
		pointerPageFile.save(0, nCommittedJournalEntries);

		if (nCommittedJournalEntries > 128)
			applyJournal();
	}

	/**
	 * Shortens the journal by applying them to page file.
	 */
	public synchronized void applyJournal() throws IOException {

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
	public synchronized Bytes load(Integer pointer) throws IOException {
		int jp = findPageInJournal(pointer);
		if (jp < 0)
			return pageFile.load(pointer);
		else
			return journalEntries.get(jp).bytes;
	}

	@Override
	public synchronized void save(Integer pointer, Bytes bytes) throws IOException {
		int jp = findDirtyPageInJournal(pointer);

		if (jp < 0) {
			jp = journalEntries.size();
			journalEntries.add(new JournalEntry(pointer, null));
		}

		JournalEntry journalEntry = journalEntries.get(jp);
		journalEntry.bytes = bytes;
		journalPageFile.save(jp, journalEntry);
	}

	private int findPageInJournal(int pointer) {
		return findPageInJournal(pointer, 0);
	}

	private int findDirtyPageInJournal(int pointer) {
		return findPageInJournal(pointer, nCommittedJournalEntries);
	}

	private int findPageInJournal(int pointer, int start) {
		int jp1 = -1;
		for (int jp = start; jp < journalEntries.size(); jp++)
			if (journalEntries.get(jp).pointer == pointer)
				jp1 = jp;
		return jp1;
	}

}
