package suite.file;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.file.impl.Database;
import suite.os.FileUtil;

public class DatabaseTest {

	@Test
	public void testRollback() throws IOException {
		try (Database database = new Database(FileUtil.tmp + "/database")) {
			database.transact(tx -> {
				for (int i = 0; i < 65536; i++)
					tx.put(i, "sample");
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testUpdate() throws IOException {
		try (Database database = new Database(FileUtil.tmp + "/database")) {
			database.transact(tx -> {
				for (int i = 0; i < 65536; i++)
					tx.put(i, "sample");
				return true;
			});

			database.transact(tx -> {
				for (int i = 0; i < 65536; i += 2)
					tx.put(i, "updated-" + tx.get(i));
				return true;
			});

			assertEquals("updated-sample", database.transact(tx -> tx.get(0)));
			assertEquals("sample", database.transact(tx -> tx.get(1)));
			assertEquals("updated-sample", database.transact(tx -> tx.get(2)));
		}
	}

}
