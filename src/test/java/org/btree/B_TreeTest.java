package org.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Comparator;
import java.util.Random;

import org.btree.ByteBufferAccessor.ByteBufferFixedStringAccessor;
import org.btree.ByteBufferAccessor.ByteBufferIntAccessor;
import org.junit.Test;

public class B_TreeTest {

	private static final int nKeys = 256;
	private Integer keys[] = new Integer[nKeys];

	private B_Tree<Integer, String> b_tree;

	private Comparator<Integer> compare = new Comparator<Integer>() {
		public int compare(Integer i, Integer j) {
			return i - j;
		}
	};

	@Test
	public void memoryTest() {
		InMemoryPersister<B_Tree.Page<Integer>> imp = new InMemoryPersister<>();

		b_tree = new B_Tree<>(imp, imp, compare);
		b_tree.setBranchFactor(4);
		b_tree.setLeafFactor(4);
		shuffleAndTest();
	}

	@Test
	public void fileTest() throws IOException {
		String filename = "/tmp/test.bt";

		try (FileAllocator al = new FileAllocator(filename);
				FilePersister<Integer, String> fp = new FilePersister<>(
						filename //
						, new ByteBufferIntAccessor() //
						, new ByteBufferFixedStringAccessor(16));) {
			b_tree = new B_Tree<>(al, fp, compare);
			b_tree.setBranchFactor(16);
			b_tree.setLeafFactor(16);
			shuffleAndTest();
		}
	}

	private void shuffleAndTest() {

		// Shuffle the numbers
		Random random = new Random();
		for (int i = 0; i < nKeys; i++)
			keys[i] = i;
		for (int i = 0; i < nKeys; i++) {
			int j = random.nextInt(nKeys);
			Integer temp = keys[i];
			keys[i] = keys[j];
			keys[j] = temp;
		}

		addAndRemove();
	}

	public void addAndRemove() {

		// Inserting this at first makes the tree depth-balanced. Why?
		b_tree.put(Integer.MAX_VALUE, Integer.toString(Integer.MAX_VALUE));

		for (int i = 0; i < nKeys; i++)
			b_tree.put(keys[i], keys[i].toString());

		for (int i = 0; i < nKeys; i++)
			assertEquals(Integer.toString(i), b_tree.get(i));

		for (int i = 0; i < nKeys; i += 2)
			b_tree.remove(i);

		b_tree.dump(System.out);

		for (int i = 1; i < nKeys; i += 2)
			b_tree.remove(i);

		b_tree.dump(System.out);
	}

}
