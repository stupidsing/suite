package suite.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import suite.primitive.Ints;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.pair.IntIntPair;

public class ContinuedFractionTest {

	@Test
	public void test() {
		var f0 = IntIntPair.of(649, 200);
		var r = f0;

		var ints = toContinuedFraction(r);
		assertTrue(Arrays.equals(new int[] { 3, 4, 12, 4, }, ints.toArray()));

		var fx = toFraction(ints);
		assertEquals(f0, fx);
	}

	// https://en.wikipedia.org/wiki/Continued_fraction#Calculating_continued_fraction_representations
	private Ints toContinuedFraction(IntIntPair r) {
		var ib = new IntsBuilder();
		while (r.t1 != 0) {
			var ip = r.t0 / r.t1;
			r = simplify(IntIntPair.of(r.t1, r.t0 - ip * r.t1));
			ib.append(ip);
		}
		return ib.toInts();
	}

	private IntIntPair toFraction(Ints ints) {
		var p = ints.size();
		var f = IntIntPair.of(ints.get(--p), 1);
		while (0 < p)
			f = simplify(IntIntPair.of(ints.get(--p) * f.t0 + f.t1, f.t0));
		return f;
	}

	private IntIntPair simplify(IntIntPair f) {
		var gcd = gcd(f.t0, f.t1);
		return IntIntPair.of(f.t0 / gcd, f.t1 / gcd);
	}

	// a < b
	private int gcd(int a, int b) {
		var f = IntIntPair.of(a, b);
		while (f.t0 != 0)
			f.update(f.t1 % f.t0, f.t0);
		return f.t1;
	}

}
