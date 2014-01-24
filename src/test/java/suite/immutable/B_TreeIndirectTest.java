package suite.immutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.immutable.B_TreeIndirect.Pointer;
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.Util;

public class B_TreeIndirectTest {

	@Test
	public void testSingleLevel() throws IOException {
		try (B_TreeIndirect<Integer> b_tree0 = new B_TreeIndirect<Integer>( //
				"/tmp/b_tree", Util.<Integer> comparator(), SerializeUtil.intSerializer)) {
			List<Integer> stamp = Arrays.asList(0);

			B_TreeIndirect<Integer>.Holder holder = b_tree0.holder();
			holder.initialize(stamp);

			B_TreeIndirect<Integer>.Transaction transaction = holder.begin();
			int size = 14;
			for (int i = 0; i < size; i++)
				transaction.add(i);
			for (int i = size - 1; i >= 0; i--)
				transaction.remove(i);
			for (int i = 0; i < size; i++)
				transaction.add(i);

			Source<Integer> source = holder.source(transaction);
			Integer integer;

			while ((integer = source.source()) != null)
				System.out.println(integer);

			holder.commit(transaction);
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		int i = 0, size = 16;

		try (B_TreeIndirect<Pointer> b_tree0 = new B_TreeIndirect<Pointer>( //
				"/tmp/b_tree" + i++, Pointer.comparator, Pointer.serializer); //
				B_TreeIndirect<Pointer> b_tree1 = new B_TreeIndirect<Pointer>( //
						"/tmp/b_tree" + i++, Pointer.comparator, Pointer.serializer, b_tree0); //
				B_TreeIndirect<String> b_tree2 = new B_TreeIndirect<String>( //
						"/tmp/b_tree" + i++, Util.<String> comparator(), SerializeUtil.string(256), b_tree1); //
		) {
			List<Integer> stamp = Arrays.asList(0);
			stamp = B_TreeIndirect.initializeAllocator(b_tree0, stamp, size - 2);
			stamp = B_TreeIndirect.initializeAllocator(b_tree1, stamp, size / 2 * (size - 2) - 2);

			B_TreeIndirect<String>.Holder holder = b_tree2.holder();
			holder.initialize(stamp);

			B_TreeIndirect<String>.Transaction transaction = holder.begin();
			transaction.add("abc");
			transaction.add("def");
			transaction.add("ghi");

			holder.commit(transaction);

			Source<String> source = holder.source(transaction);
			String string;

			while ((string = source.source()) != null)
				System.out.println(string);

			stamp = transaction.commit();
		}
	}

}
