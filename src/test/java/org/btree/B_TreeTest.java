package org.btree;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class B_TreeTest {

	private static final int nKeys = 256;
	Integer keys[] = new Integer[nKeys];

	Persister<B_Tree<Integer, String>.Page> persister;

	Comparator<Integer> compare = new Comparator<Integer>() {
		public int compare(Integer i, Integer j) {
			return i - j;
		}
	};

	B_Tree<Integer, String> b_tree;

	@Before
	public void start() {
		persister = InMemoryPersister.create();
		b_tree = new B_Tree<Integer, String>(persister, compare);
		b_tree.setBranchFactor(4);

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
	}

	@Test
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
