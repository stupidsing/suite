package suite.file.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class JournalledFileFactory {

	private static class JournalEntry {
		private int pointer;
		private Bytes bytes;

		private JournalEntry(int pointer, Bytes bytes) {
			this.pointer = pointer;
			this.bytes = bytes;
		}
	}

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
				dataOutput.writeInt(pageSize);
				bytesSerializer.write(dataOutput, journalEntry.bytes);
			}
		};

		PageFile dataFile = df;
		SerializedPageFile<JournalEntry> journalPageFile = SerializedFileFactory.serialized(jpf, journalEntrySerializer);
		SerializedPageFile<Integer> pointerPageFile = SerializedFileFactory.serialized(ppf, Serialize.int_);
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
				nCommittedJournalEntries = 0;
				pointerPageFile.save(0, nCommittedJournalEntries);
				pointerPageFile.sync();

				// write back entries for next commit
				for (int jp = 0; jp < journalEntries.size(); jp++)
					journalPageFile.save(jp, journalEntries.get(jp));
			}

			public synchronized Bytes load(int pointer) {
				int jp = findPageInJournal(pointer);
				if (jp < 0)
					return dataFile.load(pointer);
				else
					return journalEntries.get(jp).bytes;
			}

			public synchronized void save(int pointer, Bytes bytes) {
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
					if (Objects.equals(journalEntries.get(jp).pointer, pointer))
						jp1 = jp;
				return jp1;
			}
		};
	}

}
