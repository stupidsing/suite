package suite.immutable;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.immutable.B_TreeIndirect.Pointer;
import suite.util.SerializeUtil;
import suite.util.Util;

public class B_TreeIndirectTest {

	@Test
	public void test() throws FileNotFoundException {
		int i = 0;

		B_TreeIndirect<Pointer> b_tree0 = new B_TreeIndirect<Pointer>( //
				"/tmp/b_tree" + i++ //
				, Pointer.comparator //
				, Pointer.serializer);
		B_TreeIndirect<Pointer> b_tree1 = new B_TreeIndirect<Pointer>( //
				"/tmp/b_tree" + i++ //
				, Pointer.comparator //
				, Pointer.serializer //
				, b_tree0);
		B_TreeIndirect<String> b_tree2 = new B_TreeIndirect<String>( //
				"/tmp/b_tree" + i++ //
				, Util.<String> comparator() //
				, SerializeUtil.string(256) //
				, b_tree1);

		List<Integer> chain = Arrays.asList(0);
		chain = B_TreeIndirect.initializeAllocator(b_tree0, chain, 400);
		chain = B_TreeIndirect.initializeAllocator(b_tree1, chain, 400 * 400);
		chain = b_tree2.initialize(chain);

		B_TreeIndirect<String>.Transaction transaction = b_tree2.transaction(chain);
		transaction.add("abc");
		transaction.add("def");
		transaction.add("ghi");

		chain = transaction.commit();
	}

}
