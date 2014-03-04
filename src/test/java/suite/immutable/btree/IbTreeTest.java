package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.immutable.btree.IbTree;
import suite.immutable.btree.IbTreeBuilder;
import suite.immutable.btree.IbTree.Pointer;
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
	public void testSingleLevel() throws IOException {
		try (IbTree<Integer> ibTree = builder.buildTree(FileUtil.tmp + "/ibTree" //
		, Util.<Integer> comparator(), SerializeUtil.intSerializer, null)) {
			IbTree<Integer>.Holder holder = ibTree.holder();
			holder.commit(ibTree.create());

			IbTree<Integer>.Transaction transaction = holder.begin();
			int size = ibTree.guaranteedCapacity();
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
			IbTree<String>.Holder holder = ibTree2.holder();
			holder.commit(ibTree2.create());

			int size = ibTree2.guaranteedCapacity();

			List<String> list = new ArrayList<>();
			for (int k = 0; k < size; k++)
				list.add("KEY-" + To.hex4(k));

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
