package suite.file;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.Defaults;
import suite.file.impl.Database;
import suite.util.Fail;

public class DatabaseTest {

	private int nRecords = 1000;

	@Test
	public void testRollback() throws IOException {
		try (var database = new Database(Defaults.tmp("database"))) {
			database.transact(tx -> {
				for (var i = 0; i < nRecords; i++)
					tx.put(i, "sample");
				return Fail.t();
			});
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testUpdate() throws IOException {
		try (var database = new Database(Defaults.tmp("database"))) {
			database.transact(tx -> {
				for (var i = 0; i < nRecords; i++)
					tx.put(i, "sample");
				return true;
			});

			database.transact(tx -> {
				for (var i = 0; i < nRecords; i += 4)
					tx.put(i, "updated-" + tx.get(i));
				for (var i = 1; i < nRecords; i += 4)
					tx.remove(i);
				return true;
			});

			assertEquals("updated-sample", database.transact(tx -> tx.get(0)));
			assertEquals((String) null, database.transact(tx -> tx.get(1)));
			assertEquals("sample", database.transact(tx -> tx.get(2)));
			assertEquals("sample", database.transact(tx -> tx.get(3)));
		}
	}

}
