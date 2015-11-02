package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.fs.KeyValueStoreMutator;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeMutator;
import suite.os.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.Serialize;
import suite.util.Util;

public class Database implements Closeable {

	JournalledPageFile pageFile;
	TransactionManager<Integer, String> txm;

	public Database() throws IOException {
		pageFile = new JournalledPageFileImpl(FileUtil.tmp + "/database", PageFile.defaultPageSize);

		txm = new TransactionManager<>(() -> new LazyIbTreeMutator<>( //
				pageFile //
				, Util.comparator() //
				, Serialize.int_ //
				, Serialize.string(64)));
	}

	@Override
	public void close() throws IOException {
		pageFile.commit();
		pageFile.close();
	}

	public <T> T transact(Fun<KeyValueStoreMutator<Integer, String>, T> callback) throws IOException {
		try {
			return txm.begin(callback);
		} finally {
			pageFile.commit();
		}
	}

}
