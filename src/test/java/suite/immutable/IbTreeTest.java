package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.immutable.IbTree.Pointer;
import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.Util;

public class IbTreeTest {

	private int maxBranchFactor = 16;
	private IbTreeBuilder builder = new IbTreeBuilder(maxBranchFactor);

	@Test
	public void testSingleLevel() throws IOException {
		try (IbTree<Integer> ibTree0 = builder.buildTree(FileUtil.tmp + "/ibTree" //
		, Util.<Integer> comparator(), SerializeUtil.intSerializer, null)) {
			List<Integer> stamp = Arrays.asList(0);

			IbTree<Integer>.Holder holder = ibTree0.holder();
			holder.create(stamp);

			IbTree<Integer>.Transaction transaction = holder.begin();
			int size = maxBranchFactor - 2;
			for (int i = 0; i < size; i++)
				transaction.add(i);
			for (int i = size - 1; i >= 0; i--)
				transaction.remove(i);
			for (int i = 0; i < size; i++)
				transaction.add(i);

			assertEquals(size, dumpAndCount(transaction));
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		int i = 0;
		String f0 = FileUtil.tmp + "/ibTree" + i++;
		String f1 = FileUtil.tmp + "/ibTree" + i++;
		String f2 = FileUtil.tmp + "/ibTree" + i++;

		try (IbTree<Pointer> ibTree0 = builder.buildPointerTree(f0);
				IbTree<Pointer> ibTree1 = builder.buildPointerTree(f1, ibTree0);
				IbTree<String> ibTree2 = builder.buildTree(f2, Util.<String> comparator(), SerializeUtil.string(16), ibTree1)) {
			List<Integer> stamp = Arrays.asList(0);
			IbTree<String>.Holder holder = ibTree2.holder();

			// To project the growth of each tree generation, we need to find
			// out the minimum tree size that would run more than n pages,
			// caused by any possible split scenario. This is calculated using a
			// conservative approach:
			//
			// First, we relate the number of branches in nodes to the size of
			// the tree. For each branch node, it occupy 1 child of its parent,
			// and create children at the number of branch factor. Therefore its
			// "gain" is its branch factor minus 1. The tree root is a single
			// entry, thus the sum of all "gains" result plus 1 in the total
			// number of leave nodes.
			//
			// Second, we find the smallest tree for n pages. 1 page is used as
			// the root which has 2 children at minimum. Other pages should have
			// half of branch factor at minimum.
			//
			// Third, to cause page exhaustion at next insert, it require a
			// split to occur. Therefore 1 page should be at its maximum size.
			// This adds in half of branch factor minus 1 of nodes.
			//
			// The final result needs to be minus by 1 to exclude the guard node
			// at rightmost of the tree.
			//
			// In formula, minimum number of nodes causing split:
			// 1 + 1 + (size - 1) * (minBranchFactor - 1) + minBranchFactor - 1
			// = size * (minBranchFactor - 1) + 2
			int minBranchFactor = maxBranchFactor / 2;
			int size;
			stamp = ibTree1.holder().createAllocator(stamp, size = maxBranchFactor - 2);
			stamp = ibTree2.holder().createAllocator(stamp, size = size * (minBranchFactor - 1) + 2);
			size = size * (minBranchFactor - 1) + 2;

			holder.create(stamp);

			List<String> list = new ArrayList<>();
			for (int k = 0; k < size; k++)
				list.add("KEY-" + Util.hex4(k));

			Collections.shuffle(list);

			IbTree<String>.Transaction transaction0 = holder.begin();
			for (String s : list)
				transaction0.add(s);
			holder.commit(transaction0);

			assertEquals(size, dumpAndCount(holder.begin()));

			Collections.shuffle(list);

			IbTree<String>.Transaction transaction1 = holder.begin();
			for (String s : list)
				transaction1.remove(s);
			holder.commit(transaction1);

			assertEquals(0, dumpAndCount(holder.begin()));
		}
	}

	private int dumpAndCount(IbTree<?>.Transaction transaction) {
		Source<?> source = transaction.source();
		Object object;
		int count = 0;

		while ((object = source.source()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

}
