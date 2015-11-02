package suite.btree;

import java.io.IOException;

import org.junit.Test;

import suite.file.impl.Database;

public class DatabaseTest {

	@Test
	public void testCommit() throws IOException {
		try (Database database = new Database()) {
			System.out.println(database.transact(tx -> {
				tx.put(0, "sample");
				return tx.get(0);
			}));
		}
	}

	@Test
	public void testRollback() throws IOException {
		try (Database database = new Database()) {
			database.transact(tx -> {
				tx.put(0, "sample");
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {
		}
	}

}
