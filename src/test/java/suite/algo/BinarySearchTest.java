package suite.algo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import primal.primitive.IntVerbs.NewInt;
import primal.primitive.Int_Int;

public class BinarySearchTest {

	@Test
	public void testAsc() {
		testAsc(0);
		testAsc(1);
		testAsc(10);
		testAsc(20000);
	}

	@Test
	public void testDesc() {
		testDesc(0);
		testDesc(1);
		testDesc(10);
		testDesc(20000);
	}

	private void testAsc(int l) {
		Int_Int f = i -> i;
		var is = NewInt.array(l, f);

		assertEquals(0, searchAsc(is, Integer.MIN_VALUE));
		assertEquals(l, searchAsc(is, Integer.MAX_VALUE));

		for (var i = 0; i < l; i++)
			assertEquals(i, searchAsc(is, f.apply(i)));
	}

	private void testDesc(int l) {
		Int_Int f = i -> l + 1 - i;
		var is = NewInt.array(l, f);

		assertEquals(l - 1, searchDesc(is, Integer.MIN_VALUE));
		assertEquals(-1, searchDesc(is, Integer.MAX_VALUE));

		for (var i = 0; i < l; i++)
			assertEquals(i, searchDesc(is, f.apply(i)));
	}

	// in an ascending sequence,
	// find the leftest value in is that is greater than or equal to i.
	// if no such value, return is.length.
	private int searchAsc(int[] is, int i) {
		var s = 0;
		var e = is.length;
		int l, mid;

		while (0 < (l = e - s))
			if (i <= is[mid = s + l / 2])
				e = mid;
			else
				s = mid + 1;

		return s;
	}

	// in a descending sequence,
	// find the rightest value in is that is greater than or equal to i.
	// if no such value, return -1.
	private int searchDesc(int[] is, int i) {
		var s = -1;
		var e = is.length - 1;
		int l, mid;

		while (0 < (l = e - s))
			if (i <= is[mid = e - l / 2])
				s = mid;
			else
				e = mid - 1;

		return e;
	}

}
