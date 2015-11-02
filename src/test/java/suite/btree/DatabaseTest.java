package suite.btree;

import java.io.IOException;

import org.junit.Test;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.file.impl.JournalledPageFileImpl;
import suite.fs.KeyValueStoreMutator;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeMutator;
import suite.os.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.Serialize;
import suite.util.Util;

public class DatabaseTest {

	@Test
	public void testCommit() throws IOException {
		System.out.println(openDatabase(tx -> {
			tx.put(0, "sample");
			return tx.get(0);
		}));
	}

	@Test
	public void testRollback() throws IOException {
		try {
			openDatabase(tx -> {
				tx.put(0, "sample");
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {
		}
	}

	private <T> T openDatabase(Fun<KeyValueStoreMutator<Integer, String>, T> callback) throws IOException {
		JournalledPageFile pageFile = new JournalledPageFileImpl(FileUtil.tmp + "/database", PageFile.defaultPageSize);

		try {
			TransactionManager<Integer, String> txm = new TransactionManager<>(() -> new LazyIbTreeMutator<>( //
					pageFile //
					, Util.comparator() //
					, Serialize.int_ //
					, Serialize.string(64)));

			return txm.begin(callback);
		} finally {
			pageFile.commit();
		}
	}

}
