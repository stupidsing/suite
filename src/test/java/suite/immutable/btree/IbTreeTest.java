package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.fs.KeyDataStoreMutator;
import suite.immutable.btree.impl.IbTreeBuilder;
import suite.immutable.btree.impl.IbTreeConfiguration;
import suite.immutable.btree.impl.IbTreeImpl;
import suite.immutable.btree.impl.IbTreeStack;
import suite.os.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.To;
import suite.util.Util;

public class IbTreeTest {

	private int pageSize = 4096;

	@Test
	public void testSimple() throws IOException {
		IbTreeConfiguration<Integer> config = createIbTreeConfiguration("ibTree-stack", Serialize.int_);
		config.setCapacity(65536);

		try (IbTreeStack<Integer> ibTreeStack = new IbTreeStack<>(config)) {
			IbTree<Integer> ibTree = ibTreeStack.getIbTree();
			ibTree.create().end(true);
			KeyDataStoreMutator<Integer> mutator = ibTree.begin();

			for (int i = 0; i < 32; i++)
				mutator.put(i, i);

			// mutator.dump(System.out);

			System.out.println(To.list(mutator.keys(3, 10)));
		}
	}

	@Test
	public void testSingleLevel() throws IOException {
		IbTreeConfiguration<Integer> config = createIbTreeConfiguration("ibTree-single", Serialize.int_);

		IbTreeBuilder builder = new IbTreeBuilder(config);

		try (IbTree<Integer> ibTree = builder.buildTree(FileUtil.tmp + "/ibTree-single", config, null)) {
			ibTree.create().end(true);

			KeyDataStoreMutator<Integer> mutator = ibTree.begin();
			int size = ibTree.guaranteedCapacity();
			for (int i = 0; i < size; i++)
				mutator.putTerminal(i);
			for (int i = size - 1; 0 <= i; i--)
				mutator.remove(i);
			for (int i = 0; i < size; i++)
				mutator.putTerminal(i);

			assertEquals(size, dumpAndCount(mutator));
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		IbTreeConfiguration<String> config = createIbTreeConfiguration("ibTree-multi", Serialize.string(16));

		IbTreeBuilder builder = new IbTreeBuilder(config);

		int i = 0;
		String f0 = FileUtil.tmp + "/ibTreeMulti" + i++;
		String f1 = FileUtil.tmp + "/ibTreeMulti" + i++;
		String f2 = FileUtil.tmp + "/ibTreeMulti" + i++;

		try (IbTreeImpl<Integer> ibTree0 = builder.buildAllocationIbTree(f0);
				IbTreeImpl<Integer> ibTree1 = builder.buildAllocationIbTree(f1, ibTree0);
				IbTree<String> ibTree2 = builder.buildTree(f2, config, ibTree1)) {
			test(ibTree2);
		}
	}

	@Test
	public void testStack() throws IOException {
		IbTreeConfiguration<String> config = createIbTreeConfiguration("ibTree-stack", Serialize.string(16));
		config.setCapacity(65536);

		try (IbTreeStack<String> ibTreeStack = new IbTreeStack<>(config)) {
			test(ibTreeStack.getIbTree());
		}
	}

	private int dumpAndCount(KeyDataStoreMutator<?> mutator) {
		Source<?> source = mutator.keys(null, null).source();
		Object object;
		int count = 0;

		while ((object = source.source()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

	private <Key extends Comparable<? super Key>> IbTreeConfiguration<Key> createIbTreeConfiguration( //
			String name, Serializer<Key> serializer) {
		IbTreeConfiguration<Key> config = new IbTreeConfiguration<>();
		config.setComparator(Util.<Key> comparator());
		config.setFilenamePrefix(FileUtil.tmp + "/" + name);
		config.setPageSize(pageSize);
		config.setSerializer(serializer);
		config.setMaxBranchFactor(16);
		return config;
	}

	private void test(IbTree<String> ibTree) {
		ibTree.create().end(true);

		int size = ibTree.guaranteedCapacity();

		List<String> list = new ArrayList<>();
		for (int k = 0; k < size; k++)
			list.add("KEY-" + To.hex4(k));

		Collections.shuffle(list);

		// During each mutation, some new pages are required before old
		// pages can be discarded during commit. If we update too much data,
		// we would run out of allocatable pages. Here we limit ourself to
		// updating 25 keys each.

		for (List<String> subset : Util.splitn(list, 25)) {
			KeyDataStoreMutator<String> mutator0 = ibTree.begin();
			for (String s : subset)
				mutator0.putTerminal(s);
			mutator0.end(true);
		}

		assertEquals(size, dumpAndCount(ibTree.begin()));

		Collections.shuffle(list);

		for (List<String> subset : Util.splitn(list, 25)) {
			KeyDataStoreMutator<String> mutator1 = ibTree.begin();
			for (String s : subset)
				mutator1.remove(s);
			mutator1.end(true);
		}

		assertEquals(0, dumpAndCount(ibTree.begin()));
	}

}
