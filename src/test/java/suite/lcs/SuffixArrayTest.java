package suite.lcs;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import suite.primitive.Ints_;

public class SuffixArrayTest {

	@Test
	public void test() {
		var s = "banana";
		var orderByStart = suffixArray(s);

		assertTrue(orderByStart[5] == 0);
		assertTrue(orderByStart[3] == 1);
		assertTrue(orderByStart[1] == 2);
		assertTrue(orderByStart[0] == 3);
		assertTrue(orderByStart[4] == 4);
		assertTrue(orderByStart[2] == 5);
	}

	private int[] suffixArray(String s) {
		var length = s.length();

		int[] orderByStart = null;
		var k2 = 1;

		do {
			var orderByStart_ = orderByStart;
			var k_ = k2 / 2;

			class Key implements Comparable<Key> {
				private int start;

				private Key(int start) {
					this.start = start;
				}

				public boolean equals(Object object) {
					return object instanceof Key && start == ((Key) object).start;
				}

				public int compareTo(Key key) {
					var start0 = start;
					var start1 = key.start;
					if (0 < k_) {
						var c = 0;
						c = c == 0 ? orderByStart_[start0] - orderByStart_[start1] : c;
						if (c == 0) {
							var i0 = start0 + k_;
							var i1 = start1 + k_;
							var order0 = i0 < length ? orderByStart_[i0] : -1;
							var order1 = i1 < length ? orderByStart_[i1] : -1;
							c = order0 - order1;
						}
						return c;
					} else
						return Character.compare(s.charAt(start0), s.charAt(start1));
				}
			}

			var keys = new ArrayList<Key>();
			Ints_.for_(0, length).sink(i -> keys.add(new Key(i)));

			Collections.sort(keys);

			orderByStart = new int[length];
			var key_ = keys.get(0);
			var counter = 0;

			for (var key : keys) {
				if (key_.compareTo(key) < 0)
					counter++;
				orderByStart[(key_ = key).start] = counter;
			}

			k2 *= 2;
		} while (k2 / 2 < length);

		return orderByStart;
	}

}
