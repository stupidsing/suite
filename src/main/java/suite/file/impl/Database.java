package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.fs.KeyValueMutator;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeStore;
import suite.node.util.Singleton;
import suite.object.Object_;
import suite.serialize.Serialize;
import suite.streamlet.FunUtil.Fun;

public class Database implements Closeable {

	private Serialize serialize = Singleton.me.serialize;

	private JournalledPageFile journalledPageFile;
	private TransactionManager<Integer, String> transactionManager;

	public Database(Path path) {
		journalledPageFile = JournalledFileFactory.journalled(path, PageFile.defaultPageSize);

		transactionManager = new TransactionManager<>(() -> LazyIbTreeStore.ofExtent( //
				journalledPageFile, //
				Object_::compare, //
				serialize.int_, //
				serialize.variableLengthString));
	}

	@Override
	public void close() throws IOException {
		journalledPageFile.commit();
		journalledPageFile.close();
	}

	public <T> T transact(Fun<KeyValueMutator<Integer, String>, T> callback) {
		try {
			return transactionManager.begin(callback);
		} finally {
			journalledPageFile.commit();
		}
	}

}
