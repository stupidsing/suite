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
	public void test0() throws IOException {
		try (B_TreeIndirect<Integer> b_tree0 = new B_TreeIndirect<Integer>( //
				"/tmp/b_tree", Util.<Integer> comparator(), SerializeUtil.intSerializer)) {
			List<Integer> chain = Arrays.asList(0);
			chain = b_tree0.initialize(chain);

			B_TreeIndirect<Integer>.Transaction transaction = b_tree0.transaction(chain);
			int size = 14;
			for (int i = 0; i < size; i++)
				transaction.add(i);
			for (int i = size - 1; i >= 0; i--)
				transaction.remove(i);
			for (int i = 0; i < size; i++)
				transaction.add(i);

			Source<Integer> source = b_tree0.source(transaction);
			Integer integer;

			while ((integer = source.source()) != null)
				System.out.println(integer);

			chain = transaction.commit();
		}
	}

	@Test
	public void test() throws IOException {
		int i = 0;

		try (B_TreeIndirect<Pointer> b_tree0 = new B_TreeIndirect<Pointer>( //
				"/tmp/b_tree" + i++, Pointer.comparator, Pointer.serializer); //
				B_TreeIndirect<Pointer> b_tree1 = new B_TreeIndirect<Pointer>( //
						"/tmp/b_tree" + i++, Pointer.comparator, Pointer.serializer, b_tree0); //
				B_TreeIndirect<String> b_tree2 = new B_TreeIndirect<String>( //
						"/tmp/b_tree" + i++, Util.<String> comparator(), SerializeUtil.string(256), b_tree1); //
		) {
			List<Integer> chain = Arrays.asList(0);
			chain = B_TreeIndirect.initializeAllocator(b_tree0, chain, 14);
			chain = B_TreeIndirect.initializeAllocator(b_tree1, chain, 8 * 14);
			chain = b_tree2.initialize(chain);

			B_TreeIndirect<String>.Transaction transaction = b_tree2.transaction(chain);
			transaction.add("abc");
			transaction.add("def");
			transaction.add("ghi");

			chain = transaction.commit();
		}
	}

}
