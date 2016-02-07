package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;

public class LazyFunInterpreterTest {

	@Test
	public void testDecons() {
		String expr = "let l := (1; 3;) >> if (l = `$a; $b;`) then b else error";
		assertEquals(Int.of(3), new LazyFunInterpreter().lazy(Suite.parse(expr)).get());
	}

	@Test
	public void testFibonacci() {
		String expr = "define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}";
		assertEquals(Int.of(144), new LazyFunInterpreter().lazy(Suite.parse(expr)).get());
	}

}
