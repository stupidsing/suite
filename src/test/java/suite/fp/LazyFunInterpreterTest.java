package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;

public class LazyFunInterpreterTest {

	@Test
	public void testShortCircuitEvaluation() {
		String expr = "(if true then (p => 0) else (p => error)) {p}";
		assertEquals(Int.of(0), new LazyFunInterpreter().lazy(Suite.parse(expr)).get());
	}

	@Test
	public void testFibonacci() {
		String expr = "fib := (n => if (n > 1) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}";
		assertEquals(Int.of(144), new LazyFunInterpreter().lazy(Suite.parse(expr)).get());
	}

}
