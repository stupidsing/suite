package suite.file.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import suite.adt.IntObjPair;
import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.streamlet.IntObjOutlet;
import suite.streamlet.IntObjStreamlet;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class JournalledFileFactory {

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
		Serializer<Bytes> bytesSerializer = Serialize.bytes(pageSize);

		Serializer<JournalEntry> journalEntrySerializer = new Serializer<JournalEntry>() {
			public JournalEntry read(DataInput dataInput) throws IOException {
				int pointer = dataInput.readInt();
				Bytes bytes = bytesSerializer.read(dataInput);
				return new JournalEntry(pointer, bytes);
			}

			public void write(DataOutput dataOutput, JournalEntry journalEntry) throws IOException {
				dataOutput.writeInt(journalEntry.pointer);
				bytesSerializer.write(dataOutput, journalEntry.bytes);
			}
		};

		PageFile dataFile = df;
		SerializedPageFile<JournalEntry> journalPageFile = SerializedFileFactory.serialized(jpf, journalEntrySerializer);
		SerializedPageFile<Integer> pointerPageFile = SerializedFileFactory.serialized(ppf, Serialize.int_);
		int nCommittedJournalEntries0 = pointerPageFile.load(0);
		JournalEntries journalEntries = new JournalEntries();

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
				for (IntObjPair<JournalEntry> pair : journalEntries.range(nCommittedJournalEntries)) {
					JournalEntry journalEntry = pair.t1;
					dataFile.save(journalEntry.pointer, journalEntry.bytes);
				}

				if (128 < (nCommittedJournalEntries = journalEntries.size()))
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

				if (1024 < nCommittedJournalEntries)
					applyJournal();
			}

			/**
			 * Shortens the journal by applying them to page file.
			 */
			public synchronized void applyJournal() {

				// make sure all changes are written to main file
				dataFile.sync();

				// clear all committed entries
				journalEntries.remove(0, nCommittedJournalEntries);

				// reset committed pointer
				pointerPageFile.save(0, nCommittedJournalEntries = 0);
				pointerPageFile.sync();

				// write back entries for next commit
				for (IntObjPair<JournalEntry> pair : journalEntries.range(0))
					journalPageFile.save(pair.t0, pair.t1);
			}

			private IntObjPair<JournalEntry> findPageInJournal(int pointer) {
				return journalEntries.findPageInJournal(pointer, 0);
			}

			private IntObjPair<JournalEntry> findDirtyPageInJournal(int pointer) {
				return journalEntries.findPageInJournal(pointer, nCommittedJournalEntries);
			}
		};
	}

	private static class JournalEntries {
		private List<JournalEntry> jes = new ArrayList<>();

		private void add(JournalEntry je) {
			jes.add(je);
		}

		private IntObjPair<JournalEntry> findPageInJournal(int pointer, int start) {
			return range(start) //
					.filterValue(journalEntry -> journalEntry.pointer == pointer) //
					.last();
		}

		private IntObjStreamlet<JournalEntry> range(int start) {
			return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<JournalEntry>() {
				private int index = start;
				private int end = size();

				public boolean source2(IntObjPair<JournalEntry> pair) {
					boolean b = index < end;
					if (b) {
						pair.t0 = index;
						pair.t1 = jes.get(index++);
					}
					return b;
				}
			}));
		}

		private void remove(int start, int end) {
			jes.subList(start, end).clear();
		}

		private int size() {
			return jes.size();
		}
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
