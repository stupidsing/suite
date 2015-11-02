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
import suite.util.Serialize;
import suite.util.Util;

public class DatabaseTest {

	@Test
	public void testCommit() throws IOException {
		JournalledPageFile pageFile = new JournalledPageFileImpl(FileUtil.tmp + "/database", PageFile.defaultPageSize);

		try {
			KeyValueStoreMutator<Integer, String> mutator = new LazyIbTreeMutator<>( //
					pageFile //
					, Util.comparator() //
					, Serialize.int_ //
					, Serialize.string(64));
			TransactionManager<Integer, String> txm = new TransactionManager<>(() -> mutator);

			String value = txm.begin(tx -> {
				tx.put(0, "sample");
				return tx.get(0);
			});

			System.out.println(value);
		} finally {
			pageFile.commit();
		}
	}

	@Test
	public void testRollback() throws IOException {
		JournalledPageFile pageFile = new JournalledPageFileImpl(FileUtil.tmp + "/database", PageFile.defaultPageSize);

		try {
			KeyValueStoreMutator<Integer, String> mutator = new LazyIbTreeMutator<>( //
					pageFile //
					, Util.comparator() //
					, Serialize.int_ //
					, Serialize.string(64));
			TransactionManager<Integer, String> txm = new TransactionManager<>(() -> mutator);

			txm.begin(tx -> {
				tx.put(0, "sample");
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {
		} finally {
			pageFile.commit();
		}
	}

}
