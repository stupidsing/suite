package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.fs.KeyValueMutator;
import suite.fs.impl.TransactionManager;
import suite.node.util.Singleton;
import suite.object.Object_;
import suite.os.FileUtil;
import suite.persistent.LazyPbTreeStore;
import suite.serialize.Serialize;
import suite.streamlet.FunUtil.Fun;

public class Database implements Closeable {

	private Serialize ser = Singleton.me.serialize;

	private JournalledPageFile journalledPageFile;
	private TransactionManager<Integer, String> transactionManager;

	public static Database create(Path path) {
		FileUtil.deleteIfExists(path);
		FileUtil.deleteIfExists(FileUtil.ext(path, ".journal"));
		FileUtil.deleteIfExists(FileUtil.ext(path, ".pointer"));
		return open(path);
	}

	public static Database open(Path path) {
		return new Database(path);
	}

	private Database(Path path) {
		journalledPageFile = JournalledFileFactory.journalled(path, PageFile.defaultPageSize);

		transactionManager = new TransactionManager<>(() -> LazyPbTreeStore.ofExtent( //
				journalledPageFile, //
				Object_::compare, //
				ser.int_, //
				ser.variableLengthString));
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
