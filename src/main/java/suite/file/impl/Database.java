package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.fs.KeyValueStoreMutator;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeMutator;
import suite.util.FunUtil.Fun;
import suite.util.Serialize;
import suite.util.Util;

public class Database implements Closeable {

	private JournalledPageFile journalledPageFile;
	private TransactionManager<Integer, String> txm;

	public Database(String filename) throws IOException {
		journalledPageFile = new JournalledPageFileImpl(filename, PageFile.defaultPageSize);

		txm = new TransactionManager<>(() -> new LazyIbTreeMutator<>( //
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

	public <T> T transact(Fun<KeyValueStoreMutator<Integer, String>, T> callback) throws IOException {
		try {
			return txm.begin(callback);
		} finally {
			journalledPageFile.commit();
		}
	}

}
