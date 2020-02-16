package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;

public class YCombinatorTest {

	private String y = "(f => (x => f_{n => x_{x}_{n}})_{x => f_{n => x_{x}_{n}}})";

	private String lazyy = "(f => (x => f_{x_{x}})_{x => f_{x_{x}}})";

	@Test
	public void testFactorial() {
		assertEquals(Suite.parse("3628800"), Suite.evaluateFun("" //
				+ "10 | " + y + "_{fac => n => if (1 < n) then (n * fac_{n - 1}) else 1}", false));
		assertEquals(Suite.parse("3628800"), Suite.evaluateFun("" //
				+ "10 | " + lazyy + "_{fac => n => if (1 < n) then (n * fac_{n - 1}) else 1}", true));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Suite.parse("55"), Suite.evaluateFun("" //
				+ "10 | " + y + "_{fib => n => if (1 < n) then (fib_{n - 1} + fib_{n - 2}) else n}", false));
		assertEquals(Suite.parse("55"), Suite.evaluateFun("" //
				+ "10 | " + lazyy + "_{fib => n => if (1 < n) then (fib_{n - 1} + fib_{n - 2}) else n}", true));
	}

}
