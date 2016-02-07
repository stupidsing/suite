package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;

public class YCombinatorTest {

	private String y = "(f => (x => f {n => x {x} {n}}) {x => f {n => x {x} {n}}})";

	private String lazyy = "(f => (x => f {x {x}}) {x => f {x {x}}})";

	@Test
	public void testFactorial() {
		assertEquals(Suite.parse("3628800"), Suite.evaluateFun("" //
				+ "10 | " + y + " {fac => n => if (1 < n) then (n * fac {n - 1}) else 1}", false));
		assertEquals(Suite.parse("3628800"), Suite.evaluateFun("" //
				+ "10 | " + lazyy + " {fac => n => if (1 < n) then (n * fac {n - 1}) else 1}", true));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Suite.parse("55"), Suite.evaluateFun("" //
				+ "10 | " + y + " {fib => n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n}", false));
		assertEquals(Suite.parse("55"), Suite.evaluateFun("" //
				+ "10 | " + lazyy + " {fib => n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n}", true));
	}

}
