package org.btree;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Random;

import org.btree.Serializer.B_TreePageSerializer;
import org.btree.Serializer.B_TreeSuperBlockSerializer;
import org.btree.Serializer.FixedStringSerializer;
import org.btree.Serializer.IntSerializer;
import org.junit.Test;

public class B_TreeTest {

	private static final int nKeys = 1024;
	private Integer keys[] = new Integer[nKeys];

	private TestB_Tree b_tree;

	private Random random = new Random();

	private static Comparator<Integer> compare = new Comparator<Integer>() {
		public int compare(Integer i0, Integer i1) {
			return i0.compareTo(i1);
		}
	};

	private static class TestB_Tree extends B_Tree<Integer, String> {
		public TestB_Tree() {
			super(compare);
		}
	}

	@Test
	public void memoryTest() {
		InMemoryAllocPersister<TestB_Tree.SuperBlock> sbimap = new InMemoryAllocPersister<>();
		InMemoryAllocPersister<TestB_Tree.Page> pimap = new InMemoryAllocPersister<>();

		b_tree = new TestB_Tree();
		b_tree.setAllocator(pimap);
		b_tree.setSuperBlockPersister(sbimap);
		b_tree.setPagePersister(pimap);
		b_tree.setBranchFactor(4);
		b_tree.create();
		shuffleAndTest();
	}

	@Test
	public void fileTest() throws IOException {
		String prefix = "/tmp/test";
		String superBlockFilename = prefix + ".superblock";
		String pageFilename = prefix + ".pages";
		String allocMapFilename = prefix + ".alloc";

		new File(superBlockFilename).delete();
		new File(pageFilename).delete();
		new File(allocMapFilename).delete();

		b_tree = new TestB_Tree();

		B_TreeSuperBlockSerializer<Integer, String> sbs = new B_TreeSuperBlockSerializer<>(
				b_tree);
		B_TreePageSerializer<Integer, String> ps = new B_TreePageSerializer<>(
				b_tree //
				, new IntSerializer() //
				, new FixedStringSerializer(16));

		try (FileAllocator al = new FileAllocator(allocMapFilename);
				FilePersister<TestB_Tree.SuperBlock> sbp = new FilePersister<>(
						superBlockFilename, sbs);
				FilePersister<TestB_Tree.Page> pp = new FilePersister<>(
						pageFilename, ps);) {
			b_tree.setAllocator(al);
			b_tree.setSuperBlockPersister(sbp);
			b_tree.setPagePersister(pp);
			b_tree.setBranchFactor(16);
			b_tree.create();
			shuffleAndTest();
		}
	}

	private void shuffleAndTest() {
		for (int i = 0; i < nKeys; i++)
			keys[i] = i;

		addAndRemove();
	}

	private void shuffleNumbers() {
		for (int i = 0; i < nKeys; i++) {
			int j = random.nextInt(nKeys);
			Integer temp = keys[i];
			keys[i] = keys[j];
			keys[j] = temp;
		}
	}

	private void addAndRemove() {
		shuffleNumbers();

		for (int i = 0; i < nKeys; i++)
			b_tree.put(keys[i], keys[i].toString());

		for (int i = 0; i < nKeys; i++)
			assertEquals(Integer.toString(i), b_tree.get(i));

		shuffleNumbers();

		for (int i = 0; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (int i = 1; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);
	}

}
