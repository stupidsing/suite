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

	private int maxBranchFactor = 16; // Must be same as IbTree.maxBranchFactor

	@Test
	public void testSingleLevel() throws IOException {
		try (IbTree<Integer> ibTree0 = new IbTree<Integer>( //
				FileUtil.tmp + "/ibTree", Util.<Integer> comparator(), SerializeUtil.intSerializer)) {
			List<Integer> stamp = Arrays.asList(0);

			IbTree<Integer>.Holder holder = ibTree0.holder();
			holder.build(stamp);

			IbTree<Integer>.Transaction transaction = holder.begin();
			int size = maxBranchFactor - 2;
			for (int i = 0; i < size; i++)
				transaction.add(i);
			for (int i = size - 1; i >= 0; i--)
				transaction.remove(i);
			for (int i = 0; i < size; i++)
				transaction.add(i);

			Source<Integer> source = transaction.source();
			Integer integer;

			while ((integer = source.source()) != null)
				System.out.println(integer);

			holder.commit(transaction);
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		int i = 0;
		String f0 = FileUtil.tmp + "/ibTree" + i++;
		String f1 = FileUtil.tmp + "/ibTree" + i++;
		String f2 = FileUtil.tmp + "/ibTree" + i++;

		try (IbTree<Pointer> ibTree0 = new IbTree<Pointer>( //
				f0, Pointer.comparator, Pointer.serializer); //
				IbTree<Pointer> ibTree1 = new IbTree<Pointer>( //
						f1, Pointer.comparator, Pointer.serializer, ibTree0); //
				IbTree<String> ibTree2 = new IbTree<String>( //
						f2, Util.<String> comparator(), SerializeUtil.string(16), ibTree1); //
		) {
			List<Integer> stamp = Arrays.asList(0);
			int size = 2;
			stamp = IbTree.buildAllocator(ibTree0, stamp, size = size * maxBranchFactor / 2 - 2);
			stamp = IbTree.buildAllocator(ibTree1, stamp, size = size * maxBranchFactor / 2 - 2);
			size = 256;

			List<String> list = new ArrayList<>();
			for (int k = 0; k < size; k++)
				list.add("KEY-" + Util.hex2((byte) k));

			IbTree<String>.Holder holder = ibTree2.holder();
			holder.build(stamp);

			Collections.shuffle(list);

			IbTree<String>.Transaction transaction0 = holder.begin();
			for (String s : list)
				transaction0.add(s);
			holder.commit(transaction0);

			assertEquals(size, dump(holder.begin()));

			Collections.shuffle(list);

			IbTree<String>.Transaction transaction1 = holder.begin();
			for (String s : list)
				transaction1.remove(s);
			holder.commit(transaction1);

			assertEquals(0, dump(holder.begin()));
		}
	}

	private int dump(IbTree<?>.Transaction transaction) {
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
