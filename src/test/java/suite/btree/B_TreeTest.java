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
import suite.file.impl.JournalledPageFileImpl;
import suite.os.FileUtil;
import suite.util.Serialize;
import suite.util.To;
import suite.util.Util;

public class B_TreeTest {

	private static int nKeys = 1024;
	private Integer keys[] = new Integer[nKeys];

	private Random random = new Random();

	private Comparator<Integer> comparator = Util.comparator();

	@Before
	public void before() {
		for (int i = 0; i < nKeys; i++)
			keys[i] = i;
	}

	@Test
	public void testDump() throws IOException {
		int pageSize = 4096;
		Path path = FileUtil.tmp.resolve("/b_tree-dump");
		String filename = path.toString();

		Files.deleteIfExists(path);
		B_TreeBuilder<Integer, String> builder = new B_TreeBuilder<>(Serialize.int_, Serialize.string(16));

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(filename, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, true, comparator, pageSize)) {
			for (int i = 0; i < 32; i++)
				b_tree.put(i, Integer.toString(i));

			b_tree.dump(System.out);

			System.out.println(To.list(b_tree.keys(3, 10)));
		}
	}

	@Test
	public void testAccess() throws IOException {
		int pageSize = 4096;
		Path path = FileUtil.tmp.resolve("b_tree-file");
		String filename = path.toString();

		Files.deleteIfExists(path);
		B_TreeBuilder<Integer, String> builder = new B_TreeBuilder<>(Serialize.int_, Serialize.string(16));

		shuffleNumbers();

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(filename, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, true, comparator, pageSize)) {
			testStep0(jpf, b_tree);
		}

		shuffleNumbers();

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(filename, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, false, comparator, pageSize)) {
			testStep1(jpf, b_tree);
		}

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(filename, pageSize);
				B_Tree<Integer, String> b_tree = builder.build(jpf, false, comparator, pageSize)) {
			testStep2(jpf, b_tree);
		}
	}

	private void testStep0(JournalledPageFileImpl jpf, B_Tree<Integer, String> b_tree) throws IOException {
		for (int i = 0; i < nKeys; i++)
			b_tree.put(keys[i], keys[i].toString());

		jpf.commit();
		jpf.sync();

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

	private void testStep1(JournalledPageFileImpl jpf, B_Tree<Integer, String> b_tree) throws IOException {
		for (int i = 0; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		jpf.commit();
		jpf.sync();

		b_tree.dump(System.out);

		for (int i = 0; i < nKeys; i += 2)
			assertNull(b_tree.get(keys[i]));
	}

	private void testStep2(JournalledPageFileImpl jpf, B_Tree<Integer, String> b_tree) throws IOException {
		for (int i = 1; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		jpf.commit();
		jpf.sync();
		jpf.applyJournal();

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
