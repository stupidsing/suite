package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class YCombinatorTest {

	private String y = "" //
			+ "define y := f => (x => f {n => x {x} {n}}) {x => f {n => x {x} {n}}} >> ";

	@Test
	public void testFactorial() {
		assertEquals(Suite.parse("3628800"), eval(y //
				+ "10 | y {fac => n => if (n > 1) then (n * fac {n - 1}) else 1}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Suite.parse("55"), eval(y //
				+ "10 | y {fib => n => if (n > 1) then (fib {n - 1} + fib {n - 2}) else n}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
