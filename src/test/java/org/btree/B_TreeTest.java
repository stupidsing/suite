package org.btree;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Random;

import org.btree.Serializer.FixedStringSerializer;
import org.btree.Serializer.IntSerializer;
import org.junit.Test;

public class B_TreeTest {

	private static final int nKeys = 256;
	private Integer keys[] = new Integer[nKeys];

	private B_Tree<Integer, String> b_tree;

	private Comparator<Integer> compare = new Comparator<Integer>() {
		public int compare(Integer i0, Integer i1) {
			return i0.compareTo(i1);
		}
	};

	@Test
	public void memoryTest() {
		InMemoryAllocPersister<B_Tree<Integer, String>.Page> imap = new InMemoryAllocPersister<>();

		b_tree = new B_Tree<>(compare);
		b_tree.setAllocator(imap);
		b_tree.setPersister(imap);
		b_tree.setBranchFactor(4);
		b_tree.create();
		shuffleAndTest();
	}

	@Test
	public void fileTest() throws IOException {
		String filename = "/tmp/test.bt";
		String allocMapFilename = filename + ".alloc";
		new File(filename).delete();
		new File(allocMapFilename).delete();

		b_tree = new B_Tree<>(compare);

		try (FileAllocator al = new FileAllocator(allocMapFilename);
				FilePersister<Integer, String> fp = new FilePersister<Integer, String>(
						b_tree //
						, filename //
						, new IntSerializer() //
						, new FixedStringSerializer(16));) {
			b_tree.setAllocator(al);
			b_tree.setPersister(fp);
			b_tree.setBranchFactor(16);
			b_tree.create();
			shuffleAndTest();
		}
	}

	private void shuffleAndTest() {
		shuffleNumbers();
		addAndRemove();
	}

	private void shuffleNumbers() {
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

	private void addAndRemove() {
		for (int i = 0; i < nKeys; i++)
			b_tree.put(keys[i], keys[i].toString());

		for (int i = 0; i < nKeys; i++)
			assertEquals(Integer.toString(i), b_tree.get(i));

		for (int i = 0; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (int i = 1; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);
	}

}
