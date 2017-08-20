package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;

public class InterpretFunLazy0Test {

	@Test
	public void testFibonacci() {
		String expr = "define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}";
		assertEquals(Int.of(144), new InterpretFunLazy0().lazy(Suite.parse(expr)).get());
	}

}
