package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.To;
import suite.util.Util;

public class IbTreeTest {

	private int maxBranchFactor = 16;
	private int pageSize = 4096;
	private IbTreeBuilder builder = new IbTreeBuilder(maxBranchFactor, pageSize);

	@Test
	public void testSingleLevel() throws FileNotFoundException {
		try (IbTree<Integer> ibTree = builder.buildTree(FileUtil.tmp + "/ibTree" //
		, Util.<Integer> comparator(), SerializeUtil.intSerializer, null)) {
			ibTree.create().commit();

			IbTree<Integer>.Transaction transaction = ibTree.begin();
			int size = ibTree.guaranteedCapacity();
			for (int i = 0; i < size; i++)
				transaction.put(i);
			for (int i = size - 1; i >= 0; i--)
				transaction.remove(i);
			for (int i = 0; i < size; i++)
				transaction.put(i);

			assertEquals(size, dumpAndCount(transaction));
		}
	}

	@Test
	public void testMultipleLevels() throws FileNotFoundException {
		int i = 0;
		String f0 = FileUtil.tmp + "/ibTree" + i++;
		String f1 = FileUtil.tmp + "/ibTree" + i++;
		String f2 = FileUtil.tmp + "/ibTree" + i++;

		try (IbTree<Integer> ibTree0 = builder.buildPointerTree(f0);
				IbTree<Integer> ibTree1 = builder.buildPointerTree(f1, ibTree0);
				IbTree<String> ibTree2 = builder.buildTree(f2, Util.<String> comparator(), SerializeUtil.string(16), ibTree1)) {
			ibTree2.create().commit();

			int size = ibTree2.guaranteedCapacity();

			List<String> list = new ArrayList<>();
			for (int k = 0; k < size; k++)
				list.add("KEY-" + To.hex4(k));

			Collections.shuffle(list);

			// During each transaction, some new pages are required before old
			// pages can be discarded during commit. If we update too much data,
			// we would run out of allocatable pages. Here we limit ourselves to
			// updating 25 keys each.

			for (List<String> subset : Util.splitn(list, 25)) {
				IbTree<String>.Transaction transaction0 = ibTree2.begin();
				for (String s : subset)
					transaction0.put(s);
				transaction0.commit();
			}

			assertEquals(size, dumpAndCount(ibTree2.begin()));

			Collections.shuffle(list);

			for (List<String> subset : Util.splitn(list, 25)) {
				IbTree<String>.Transaction transaction1 = ibTree2.begin();
				for (String s : subset)
					transaction1.remove(s);
				transaction1.commit();
			}

			assertEquals(0, dumpAndCount(ibTree2.begin()));
		}
	}

	private int dumpAndCount(IbTree<?>.Transaction transaction) {
		Source<?> source = transaction.keys();
		Object object;
		int count = 0;

		while ((object = source.source()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

}
