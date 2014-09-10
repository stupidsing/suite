package suite.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import suite.btree.impl.B_TreeFactory;
import suite.file.JournalledPageFileImpl;
import suite.util.FileUtil;
import suite.util.Pair;
import suite.util.SerializeUtil;

public class B_TreeTest {

	private static int nKeys = 1024;
	private Integer keys[] = new Integer[nKeys];

	private Random random = new Random();

	private Comparator<Integer> comparator = (i0, i1) -> i0.compareTo(i1);

	@Before
	public void before() {
		for (int i = 0; i < nKeys; i++)
			keys[i] = i;
	}

	@Test
	public void fileTest() throws IOException {
		int pageSize = 4096;
		String pathName = FileUtil.tmp + "/test-btree";

		Files.deleteIfExists(Paths.get(pathName));
		B_TreeFactory<Integer, String> factory = new B_TreeFactory<>(SerializeUtil.intSerializer, SerializeUtil.string(16));

		shuffleNumbers();

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(pathName, pageSize);
				B_Tree<Integer, String> b_tree = factory.produce(jpf, true, comparator, pageSize)) {
			testStep0(jpf, b_tree);
		}

		shuffleNumbers();

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(pathName, pageSize);
				B_Tree<Integer, String> b_tree = factory.produce(jpf, false, comparator, pageSize)) {
			testStep1(jpf, b_tree);
		}

		try (JournalledPageFileImpl jpf = new JournalledPageFileImpl(pathName, pageSize);
				B_Tree<Integer, String> b_tree = factory.produce(jpf, false, comparator, pageSize)) {
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
