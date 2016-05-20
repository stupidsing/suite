package suite.file;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.file.impl.Database;
import suite.os.FileUtil;

public class DatabaseTest {

	private int nRecords = 1000;

	@Test
	public void testRollback() throws IOException {
		try (Database database = new Database(FileUtil.tmp.resolve("database"))) {
			database.transact(tx -> {
				for (int i = 0; i < nRecords; i++)
					tx.put(i, "sample");
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testUpdate() throws IOException {
		try (Database database = new Database(FileUtil.tmp.resolve("database"))) {
			database.transact(tx -> {
				for (int i = 0; i < nRecords; i++)
					tx.put(i, "sample");
				return true;
			});

			database.transact(tx -> {
				for (int i = 0; i < nRecords; i += 4)
					tx.put(i, "updated-" + tx.get(i));
				for (int i = 1; i < nRecords; i += 4)
					tx.remove(i);
				return true;
			});

			assertEquals("updated-sample", database.transact(tx -> tx.get(0)));
			assertEquals(null, database.transact(tx -> tx.get(1)));
			assertEquals("sample", database.transact(tx -> tx.get(2)));
			assertEquals("sample", database.transact(tx -> tx.get(3)));
		}
	}

}
