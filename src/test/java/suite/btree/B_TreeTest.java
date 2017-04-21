package suite.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import suite.adt.Pair;
import suite.btree.impl.B_TreeBuilder;
import suite.file.JournalledPageFile;
import suite.file.impl.JournalledFileFactory;
import suite.primitive.Bytes;
import suite.sample.Profiler;
import suite.util.Serialize;
import suite.util.TempDir;
import suite.util.To;
import suite.util.Util;

public class B_TreeTest {

	private static int nKeys = 1024;

	private Comparator<Integer> comparator = Util.comparator();
	private Random random = new Random();
	private int[] keys;

	@Before
	public void before() {
		keys = To.intArray(nKeys, i -> i);
	}

	@Test
	public void testDump() throws IOException {
		int pageSize = 4096;
		Path path = TempDir.resolve("b_tree-dump");

		Files.deleteIfExists(path);
		B_TreeBuilder<Integer, String> builder = new B_TreeBuilder<>(Serialize.int_, Serialize.string(16));

		try (JournalledPageFile jpf = JournalledFileFactory.journalled(path, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, comparator, pageSize)) {
			b_tree.create();

			for (int i = 0; i < 32; i++)
				b_tree.put(i, Integer.toString(i));

			b_tree.dump(System.out);

			System.out.println(To.list(b_tree.keys(3, 10)));
			jpf.commit();
		}
	}

	@Test
	public void testAccess() throws IOException {
		int pageSize = 4096;
		Path path = TempDir.resolve("b_tree-file");

		Files.deleteIfExists(path);
		B_TreeBuilder<Integer, String> builder = new B_TreeBuilder<>(Serialize.int_, Serialize.string(16));

		shuffleNumbers();

		try (JournalledPageFile jpf = JournalledFileFactory.journalled(path, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, comparator, pageSize)) {
			b_tree.create();
			testStep0(b_tree);
			jpf.commit();
			jpf.sync();
		}

		shuffleNumbers();

		try (JournalledPageFile jpf = JournalledFileFactory.journalled(path, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, comparator, pageSize)) {
			testStep1(b_tree);
			jpf.commit();
			jpf.sync();
		}

		try (JournalledPageFile jpf = JournalledFileFactory.journalled(path, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, comparator, pageSize)) {
			testStep2(b_tree);
			jpf.commit();
			jpf.sync();
			jpf.applyJournal();
		}
	}

	@Test
	public void testInsertPerformance() throws IOException {
		int nKeys = 16384;
		keys = To.intArray(nKeys, i -> i);
		int pageSize = 4096;
		Path path = TempDir.resolve("b_tree-file");

		for (int i = 0; i < nKeys; i++) {
			int j = random.nextInt(nKeys);
			Integer temp = keys[i];
			keys[i] = keys[j];
			keys[j] = temp;
		}

		Files.deleteIfExists(path);
		B_TreeBuilder<Integer, Bytes> builder = new B_TreeBuilder<>(Serialize.int_, Serialize.bytes(64));

		try (JournalledPageFile jpf = JournalledFileFactory.journalled(path, pageSize);
				B_Tree<Integer, Bytes> b_tree = builder.build(jpf, comparator, 9999)) {
			new Profiler().profile(() -> {
				b_tree.create();
				for (int i = 0; i < nKeys; i++) {
					int key = keys[i];
					b_tree.put(key, To.bytes(Integer.toString(key)));
				}
				jpf.commit();
				jpf.sync();
			});
		}
	}

	private void testStep0(B_Tree<Integer, String> b_tree) {
		for (int i = 0; i < nKeys; i++)
			b_tree.put(keys[i], Integer.toString(keys[i]));

		for (int i = 0; i < nKeys; i++)
			assertEquals(Integer.toString(i), b_tree.get(i));

		assertEquals(nKeys / 2, To.list(b_tree.keys(0, nKeys / 2)).size());
		int count = 0;

		for (Pair<Integer, String> entry : b_tree.range(0, nKeys)) {
			Integer key = count++;
			assertEquals(key, entry.t0);
			assertEquals(Integer.toString(key), entry.t1);
		}
	}

	private void testStep1(B_Tree<Integer, String> b_tree) {
		for (int i = 0; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (int i = 0; i < nKeys; i += 2)
			assertNull(b_tree.get(keys[i]));
	}

	private void testStep2(B_Tree<Integer, String> b_tree) {
		for (int i = 1; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (int i = 0; i < nKeys; i++)
			assertNull(b_tree.get(keys[i]));
	}

	private void shuffleNumbers() {
		for (int i = 0; i < nKeys; i++) {
			int j = random.nextInt(nKeys);
			Integer temp = keys[i];
			keys[i] = keys[j];
			keys[j] = temp;
		}
	}

}
