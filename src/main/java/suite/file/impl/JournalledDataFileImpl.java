package suite.file.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.file.DataFile;
import suite.file.JournalledDataFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

/**
 * Protect data against power loss failures by recording journals.
 *
 * @author ywsing
 */
public class JournalledDataFileImpl<Pointer> implements JournalledDataFile<Pointer> {

	private DataFile<Pointer> dataFile;
	private SerializedPageFile<JournalEntry> journalPageFile;
	private SerializedPageFile<Integer> pointerPageFile;

	private int nCommittedJournalEntries;
	private List<JournalEntry> journalEntries = new ArrayList<>();

	private Serializer<Pointer> pointerSerializer;
	private Serializer<Bytes> bytesSerializer;
	private Serializer<JournalEntry> journalEntrySerializer = new Serializer<JournalEntry>() {
		public JournalEntry read(DataInput dataInput) throws IOException {
			Pointer pointer = pointerSerializer.read(dataInput);
			Bytes bytes = bytesSerializer.read(dataInput);
			return new JournalEntry(pointer, bytes);
		}

		public void write(DataOutput dataOutput, JournalEntry journalEntry) throws IOException {
			pointerSerializer.write(dataOutput, journalEntry.pointer);
			bytesSerializer.write(dataOutput, journalEntry.bytes);
		}
	};

	private class JournalEntry {
		private Pointer pointer;
		private Bytes bytes;

		private JournalEntry(Pointer pointer, Bytes bytes) {
			this.pointer = pointer;
			this.bytes = bytes;
		}
	}

	public JournalledDataFileImpl( //
			DataFile<Pointer> df //
			, PageFile jpf //
			, PageFile ppf //
			, int pageSize //
			, Serializer<Pointer> ps) {
		dataFile = df;
		journalPageFile = new SerializedPageFileImpl<>(jpf, journalEntrySerializer);
		pointerPageFile = new SerializedPageFileImpl<>(ppf, Serialize.int_);
		pointerSerializer = ps;
		bytesSerializer = Serialize.bytes(pageSize);
		nCommittedJournalEntries = pointerPageFile.load(0);

		for (int jp = 0; jp < nCommittedJournalEntries; jp++)
			journalEntries.add(journalPageFile.load(jp));
	}

	@Override
	public void close() throws IOException {
		dataFile.close();
		journalPageFile.close();
		pointerPageFile.close();
	}

	/**
	 * Marks a snapshot that data can be recovered to.
	 */
	public synchronized void commit() {
		while (nCommittedJournalEntries < journalEntries.size()) {
			JournalEntry journalEntry = journalEntries.get(nCommittedJournalEntries++);
			dataFile.save(journalEntry.pointer, journalEntry.bytes);
		}

		if (8 < nCommittedJournalEntries)
			saveJournal();
	}

	/**
	 * Makes sure the current snapshot of data is saved and recoverable on
	 * failure, upon the return of method call.
	 */
	@Override
	public synchronized void sync() {
		journalPageFile.sync();
		saveJournal();
		pointerPageFile.sync();
	}

	private void saveJournal() {
		pointerPageFile.save(0, nCommittedJournalEntries);

		if (128 < nCommittedJournalEntries)
			applyJournal();
	}

	/**
	 * Shortens the journal by applying them to page file.
	 */
	public synchronized void applyJournal() {

		// Make sure all changes are written to main file
		dataFile.sync();

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
	public synchronized Bytes load(Pointer pointer) {
		int jp = findPageInJournal(pointer);
		if (jp < 0)
			return dataFile.load(pointer);
		else
			return journalEntries.get(jp).bytes;
	}

	@Override
	public synchronized void save(Pointer pointer, Bytes bytes) {
		int jp = findDirtyPageInJournal(pointer);

		if (jp < 0) {
			jp = journalEntries.size();
			journalEntries.add(new JournalEntry(pointer, null));
		}

		JournalEntry journalEntry = journalEntries.get(jp);
		journalEntry.bytes = bytes;
		journalPageFile.save(jp, journalEntry);
	}

	private int findPageInJournal(Pointer pointer) {
		return findPageInJournal(pointer, 0);
	}

	private int findDirtyPageInJournal(Pointer pointer) {
		return findPageInJournal(pointer, nCommittedJournalEntries);
	}

	private int findPageInJournal(Pointer pointer, int start) {
		int jp1 = -1;
		for (int jp = start; jp < journalEntries.size(); jp++)
			if (Objects.equals(journalEntries.get(jp).pointer, pointer))
				jp1 = jp;
		return jp1;
	}

}
