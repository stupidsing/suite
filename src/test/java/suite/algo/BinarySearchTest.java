package suite.algo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.primitive.Ints_;

public class BinarySearchTest {

	@Test
	public void test() {
		test(0);
		test(1);
		test(10);
		test(20000);
	}

	private void test(int size) {
		var is = Ints_.toArray(size, i -> i);
		var l = is.length;

		assertEquals(0, search(is, Integer.MIN_VALUE));
		assertEquals(l, search(is, Integer.MAX_VALUE));

		for (var i = 0; i < l; i++)
			assertEquals(i, search(is, i));
	}

	// find the leftest value in is that is greater than or equal to i.
	// if no such value, return is.length.
	private int search(int[] is, int i) {
		var s = 0;
		var e = is.length;
		int mid;

		while (s != e)
			if (i <= is[mid = s + (e - s) / 2])
				e = mid;
			else
				s = mid + 1;

		return s;
	}

}
