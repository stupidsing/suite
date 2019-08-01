package suite.persistent;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.junit.Test;

import primal.Verbs.Compare;

public class PbTreeTest {

	private int max = 32;

	private Comparator<Integer> comparator = Compare::objects;

	@Test
	public void test0() {
		var pbTree = new PbTree<>(comparator);

		for (var i = 0; i < max; i++)
			pbTree = pbTree.add(i);

		pbTree.validate();
		System.out.println(pbTree.toString());
		assertEquals(max, pbTree.streamlet().size());

		for (var i = 0; i < max; i++)
			pbTree = pbTree.remove(i);

		pbTree.validate();
		System.out.println(pbTree.toString());
		assertEquals(0, pbTree.streamlet().size());
	}

	@Test
	public void test1() {
		var pbTree = new PbTree<>(comparator);

		for (var i = 0; i < max; i += 2)
			pbTree = pbTree.add(i);
		for (var i = 1; i < max; i += 2)
			pbTree = pbTree.add(i);
		pbTree.validate();
		assertEquals(max, pbTree.streamlet().size());

		for (var i = 0; i < max; i += 2)
			pbTree = pbTree.remove(i);
		for (var i = 1; i < max; i += 2)
			pbTree = pbTree.remove(i);
		pbTree.validate();
		assertEquals(0, pbTree.streamlet().size());
	}

	@Test
	public void test2() {
		var list = new ArrayList<Integer>();
		for (var i = 0; i < max; i++)
			list.add(i);

		var pbTree = new PbTree<>(comparator);

		Collections.shuffle(list);
		for (var i : list)
			pbTree = pbTree.add(i);

		pbTree.validate();
		System.out.println(pbTree.toString());
		assertEquals(max, pbTree.streamlet().size());

		Collections.shuffle(list);
		for (var i : list)
			pbTree = pbTree.remove(i);

		pbTree.validate();
		System.out.println(pbTree.toString());
		assertEquals(0, pbTree.streamlet().size());
	}

}
