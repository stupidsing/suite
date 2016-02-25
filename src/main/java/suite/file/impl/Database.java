package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.fs.KeyValueStore;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeExtentFileMutator;
import suite.util.FunUtil.Fun;
import suite.util.Serialize;
import suite.util.Util;

public class Database implements Closeable {

	private JournalledPageFile journalledPageFile;
	private TransactionManager<Integer, String> txm;

	public Database(String filename) {
		journalledPageFile = new JournalledPageFileImpl(filename, PageFile.defaultPageSize);

		txm = new TransactionManager<>(() -> LazyIbTreeExtentFileMutator.of( //
				journalledPageFile, //
				Util.comparator(), //
				Serialize.int_, //
				Serialize.string(64)));
	}

	@Override
	public void close() throws IOException {
		journalledPageFile.commit();
		journalledPageFile.close();
	}

	public <T> T transact(Fun<KeyValueStore<Integer, String>, T> callback) {
		try {
			return txm.begin(callback);
		} finally {
			journalledPageFile.commit();
		}
	}

}
