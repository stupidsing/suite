package suite.btree;

import java.io.IOException;

import org.junit.Test;

import suite.file.PageFile;
import suite.file.impl.JournalledPageFileImpl;
import suite.fs.KeyValueStoreMutator;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeMutator;
import suite.os.FileUtil;
import suite.util.SerializeUtil;
import suite.util.Util;

public class DatabaseTest {

	@Test
	public void test() throws IOException {
		PageFile pageFile = new JournalledPageFileImpl(FileUtil.tmp + "database", PageFile.defaultPageSize);
		LazyIbTreeMutator<Integer, String> mutator = new LazyIbTreeMutator<>( //
				pageFile //
				, Util.comparator() //
				, SerializeUtil.intSerializer //
				, SerializeUtil.string(256));
		TransactionManager<Integer, String> txm = new TransactionManager<>(() -> mutator);
		KeyValueStoreMutator<Integer, String> tx = txm.begin();
		tx.put(0, "sample");
		tx.end(true);
	}

}
