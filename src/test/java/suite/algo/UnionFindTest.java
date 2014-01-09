package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class UnionFindTest {

	@Test
	public void test() {
		UnionFind<Integer> unionFind = new UnionFind<>();
		unionFind.union(4, 5);
		unionFind.union(5, 6);
		unionFind.union(1, 2);
		unionFind.union(2, 3);
		unionFind.union(3, 4);
		unionFind.union(7, 8);
		unionFind.union(0, 9);

		assertEquals(1, find(unionFind, 1, 6).size());
		assertEquals(1, find(unionFind, 7, 8).size());
	}

	private Set<Integer> find(UnionFind<Integer> unionFind, int start, int end) {
		Set<Integer> set = new HashSet<>();
		for (int i = start; i <= end; i++)
			set.add(unionFind.find(i));
		return set;
	}

}
