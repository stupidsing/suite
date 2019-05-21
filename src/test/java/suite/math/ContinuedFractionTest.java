package suite.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.pair.IntIntPair;

public class ContinuedFractionTest {

	@Test
	public void test() {
		IntIntPair r = IntIntPair.of(649, 200);
		var ib = new IntsBuilder();

		// https://en.wikipedia.org/wiki/Continued_fraction#Calculating_continued_fraction_representations
		while (r.t1 != 0) {
			var ip = r.t0 / r.t1;
			var fp = IntIntPair.of(r.t0 - ip * r.t1, r.t1);

			var gcd = gcd(fp.t0, fp.t1);
			r = IntIntPair.of(fp.t1 / gcd, fp.t0 / gcd);

			ib.append(ip);
		}

		var ints = ib.toInts();
		assertTrue(ints.size() == 4);
		assertEquals(3, ints.get(0));
		assertEquals(4, ints.get(1));
		assertEquals(12, ints.get(2));
		assertEquals(4, ints.get(3));
	}

	// a < b
	private int gcd(int a, int b) {
		while (a != 0) {
			var mod = b % a;
			b = a;
			a = mod;
		}
		return b;
	}

}
