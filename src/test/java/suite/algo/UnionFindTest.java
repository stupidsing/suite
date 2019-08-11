package suite.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import primal.primitive.adt.set.IntSet;

public class UnionFindTest {

	@Test
	public void test0() {
		var unionFind = new UnionFind<Integer>();
		unionFind.union(4, 5);
		unionFind.union(5, 6);
		unionFind.union(1, 2);
		unionFind.union(2, 3);
		unionFind.union(3, 4);
		unionFind.union(7, 8);
		unionFind.union(0, 9);

		assertTrue(unionFind.find(1) == unionFind.find(4));
		assertTrue(unionFind.find(0) != unionFind.find(7));
		assertEquals(1, find(unionFind, 1, 6).size());
		assertEquals(1, find(unionFind, 7, 8).size());
	}

	@Test
	public void test1() {
		var unionFind = new UnionFind<>();
		unionFind.union(2, 3);
		unionFind.union(6, 3);

		assertTrue(unionFind.find(2) == unionFind.find(6));
	}

	private IntSet find(UnionFind<Integer> unionFind, int start, int end) {
		var set = new IntSet();
		for (var i = start; i <= end; i++)
			set.add(unionFind.find(i));
		return set;
	}

}
