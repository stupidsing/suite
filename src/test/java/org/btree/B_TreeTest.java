\\package org.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Random;

import org.btree.Serializer.B_TreePageSerializer;
import org.btree.Serializer.B_TreeSuperBlockSerializer;
import org.btree.Serializer.FixedStringSerializer;
import org.btree.Serializer.IntSerializer;
import org.junit.Test;
import org.util.Util.Pair;

public class B_TreeTest {

	private static final int nKeys = 1024;
	private Integer keys[] = new Integer[nKeys];

	private B_Tree1 b_tree;

	private Random random = new Random();

	private static Comparator<Integer> compare = new Comparator<Integer>() {
		public int compare(Integer i0, Integer i1) {
			return i0.compareTo(i1);
		}
	};

	private static class B_Tree1 extends B_Tree<Integer, String> {
		public B_Tree1() {
			super(compare);
		}
	}

	@Test
	public void memoryTest() {
		InMemoryAllocPersister<B_Tree1.SuperBlock> sbimap = new InMemoryAllocPersister<>();
		InMemoryAllocPersister<B_Tree1.Page> pimap = new InMemoryAllocPersister<>();

		b_tree = new B_Tree1();
		b_tree.setAllocator(pimap);
		b_tree.setSuperBlockPersister(sbimap);
		b_tree.setPagePersister(pimap);
		b_tree.setBranchFactor(4);
		b_tree.create();
		shuffleAndTest();
	}

	@Test
	public void fileTest() throws IOException {
		String path = "/tmp/";
		new File(path).mkdirs();

		String prefix = path + "test-btree";
		String sbf = prefix + ".superblock";
		String amf = prefix + ".alloc";
		String pf = prefix + ".pages";

		String filenames[] = { sbf, amf, pf };
		for (String filename : filenames)
			new File(filename).delete();

		b_tree = new B_Tree1();

		B_TreeSuperBlockSerializer<Integer, String> sbs = new B_TreeSuperBlockSerializer<>(b_tree);
		B_TreePageSerializer<Integer, String> ps = new B_TreePageSerializer<>(b_tree //
				, new IntSerializer() //
				, new FixedStringSerializer(16));

		try (FileAllocator al = new FileAllocator(amf);
				FilePersister<B_Tree1.SuperBlock> sbp = new FilePersister<>(sbf, sbs);
				FilePersister<B_Tree1.Page> pp = new FilePersister<>(pf, ps) {
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

		int count = 0, half = nKeys / 2;

		for (Pair<Integer, String> entry : b_tree.range(0, half)) {
			Integer key = count++;
			assertEquals(key, entry.t1);
			assertEquals(Integer.toString(key), entry.t2);
		}

		for (Pair<Integer, String> entry : b_tree.range(half, nKeys)) {
			Integer key = count++;
			assertEquals(key, entry.t1);
			assertEquals(Integer.toString(key), entry.t2);
		}

		shuffleNumbers();

		for (int i = 0; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (int i = 1; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (Pair<Integer, String> entry : b_tree.range(0, nKeys))
			assertNull(entry);
	}

}
