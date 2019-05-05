package suite.algo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.primitive.Ints_;

public class BinarySearchTest {

	@Test
	public void test() {
		test(10);
		test(200);
	}

	private void test(int size) {
		var is = Ints_.toArray(size, i -> i);
		var l = is.length;

		assertEquals(0, search(is, Integer.MIN_VALUE));
		assertEquals(l, search(is, Integer.MAX_VALUE));

		for (var i = 0; i <= l; i++)
			assertEquals(i, search(is, i));
	}

	// find the leftest value in is that is greater than or equal to i.
	// if no such value, return is.length.
	private int search(int[] is, int i) {
		return new Object() {
			private int s(int s, int e) {
				int length = e - s;
				var mid = s + length / 2;
				if (length <= 0)
					return s;
				else if (i <= is[mid])
					return s(s, mid);
				else
					return s(mid + 1, e);
			}
		}.s(0, is.length);
	}

}
