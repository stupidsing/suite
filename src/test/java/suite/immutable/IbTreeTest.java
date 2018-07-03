package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.junit.Test;

import suite.object.Object_;

public class IbTreeTest {

	private int max = 32;

	private Comparator<Integer> comparator = Object_::compare;

	@Test
	public void test0() {
		var ibTree = new IbTree<>(comparator);

		for (var i = 0; i < max; i++)
			ibTree = ibTree.add(i);

		ibTree.validate();
		System.out.println(ibTree.toString());
		assertEquals(max, ibTree.streamlet().size());

		for (var i = 0; i < max; i++)
			ibTree = ibTree.remove(i);

		ibTree.validate();
		System.out.println(ibTree.toString());
		assertEquals(0, ibTree.streamlet().size());
	}

	@Test
	public void test1() {
		var ibTree = new IbTree<>(comparator);

		for (var i = 0; i < max; i += 2)
			ibTree = ibTree.add(i);
		for (var i = 1; i < max; i += 2)
			ibTree = ibTree.add(i);
		ibTree.validate();
		assertEquals(max, ibTree.streamlet().size());

		for (var i = 0; i < max; i += 2)
			ibTree = ibTree.remove(i);
		for (var i = 1; i < max; i += 2)
			ibTree = ibTree.remove(i);
		ibTree.validate();
		assertEquals(0, ibTree.streamlet().size());
	}

	@Test
	public void test2() {
		var list = new ArrayList<Integer>();
		for (var i = 0; i < max; i++)
			list.add(i);

		var ibTree = new IbTree<>(comparator);

		Collections.shuffle(list);
		for (var i : list)
			ibTree = ibTree.add(i);

		ibTree.validate();
		System.out.println(ibTree.toString());
		assertEquals(max, ibTree.streamlet().size());

		Collections.shuffle(list);
		for (var i : list)
			ibTree = ibTree.remove(i);

		ibTree.validate();
		System.out.println(ibTree.toString());
		assertEquals(0, ibTree.streamlet().size());
	}

}
