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
				tx.put(0, "sample");
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testUpdate() throws IOException {
		try (Database database = new Database(FileUtil.tmp + "/database")) {
			database.transact(tx -> {
				tx.put(0, "sample");
				return true;
			});

			database.transact(tx -> {
				tx.put(0, "updated-" + tx.get(0));
				return true;
			});

			String value = database.transact(tx -> tx.get(0));

			assertEquals("updated-sample", value);
		}
	}
}
