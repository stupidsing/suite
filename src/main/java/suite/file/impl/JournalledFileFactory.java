package suite.file.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.primitive.adt.pair.IntObjPair;
import suite.util.DataInput_;
import suite.util.DataOutput_;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class JournalledFileFactory {

	private static Serialize serialize = Serialize.me;

	public static JournalledPageFile journalled(Path path, int pageSize) {
		return journalled( //
				FileFactory.pageFile(path, pageSize), //
				FileFactory.pageFile(FileUtil.ext(path, ".journal"), pageSize + 4), //
				FileFactory.pageFile(FileUtil.ext(path, ".pointer"), 4), //
				pageSize);
	}

	private static JournalledPageFile journalled( //
			PageFile df, //
			PageFile jpf, //
			PageFile ppf, //
			int pageSize) {
		Serializer<Bytes> bytesSerializer = serialize.bytes(pageSize);

		Serializer<JournalEntry> journalEntrySerializer = new Serializer<>() {
			public JournalEntry read(DataInput_ dataInput) throws IOException {
				int pointer = dataInput.readInt();
				Bytes bytes = bytesSerializer.read(dataInput);
				return new JournalEntry(pointer, bytes);
			}

			public void write(DataOutput_ dataOutput, JournalEntry journalEntry) throws IOException {
				dataOutput.writeInt(journalEntry.pointer);
				bytesSerializer.write(dataOutput, journalEntry.bytes);
			}
		};

		PageFile dataFile = df;
		SerializedPageFile<JournalEntry> journalPageFile = SerializedFileFactory.serialized(jpf, journalEntrySerializer);
		SerializedPageFile<Integer> pointerPageFile = SerializedFileFactory.serialized(ppf, serialize.int_);
		int nCommittedJournalEntries0 = pointerPageFile.load(0);

		List<JournalEntry> journalEntries = new ArrayList<>();

		for (int jp = 0; jp < nCommittedJournalEntries0; jp++)
			journalEntries.add(journalPageFile.load(jp));

		return new JournalledPageFile() {
			private int nCommittedJournalEntries = nCommittedJournalEntries0;

			public void close() throws IOException {
				dataFile.close();
				journalPageFile.close();
				pointerPageFile.close();
			}

			public synchronized Bytes load(int pointer) {
				IntObjPair<JournalEntry> pair = findPageInJournal(pointer);
				if (pair != null)
					return pair.t1.bytes;
				else
					return dataFile.load(pointer);
			}

			public synchronized void save(int pointer, Bytes bytes) {
				IntObjPair<JournalEntry> pair = findDirtyPageInJournal(pointer);
				int jp;
				JournalEntry journalEntry;

				if (pair != null) {
					jp = pair.t0;
					journalEntry = pair.t1;
				} else {
					jp = journalEntries.size();
					journalEntries.add(journalEntry = new JournalEntry(pointer, null));
				}

				journalEntry.bytes = bytes;
				journalPageFile.save(jp, journalEntry);
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
			 * Makes sure the current snapshot of data is saved and recoverable
			 * on failure, upon the return of method call.
			 */
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

				// make sure all changes are written to main file
				dataFile.sync();

				// clear all committed entries
				journalEntries.subList(0, nCommittedJournalEntries).clear();

				// reset committed pointer
				pointerPageFile.save(0, nCommittedJournalEntries = 0);
				pointerPageFile.sync();

				// write back entries for next commit
				for (int jp = 0; jp < journalEntries.size(); jp++)
					journalPageFile.save(jp, journalEntries.get(jp));
			}

			private IntObjPair<JournalEntry> findPageInJournal(int pointer) {
				return findPageInJournal(pointer, 0);
			}

			private IntObjPair<JournalEntry> findDirtyPageInJournal(int pointer) {
				return findPageInJournal(pointer, nCommittedJournalEntries);
			}

			private IntObjPair<JournalEntry> findPageInJournal(int pointer, int start) {
				IntObjPair<JournalEntry> pair = null;
				for (int jp = start; jp < journalEntries.size(); jp++) {
					JournalEntry journalEntry = journalEntries.get(jp);
					if (journalEntry.pointer == pointer)
						pair = IntObjPair.of(jp, journalEntry);
				}
				return pair;
			}
		};
	}

	private static class JournalEntry {
		private int pointer;
		private Bytes bytes;

		private JournalEntry(int pointer, Bytes bytes) {
			this.pointer = pointer;
			this.bytes = bytes;
		}
	}

}
