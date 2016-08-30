package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;

public class LazyFunInterpreterTest {

	@Test
	public void testDecons() {
		String expr = "let l := (1; 3;) >> if (l = `$a; $b;`) then b else error";
		expect(expr, Int.of(3));
	}

	@Test
	public void testLets() {
		String expr = "lets (a := b + 2 # b := 1 #) >> a";
		expect(expr, Int.of(3));
	}

	@Test
	public void testFibonacci() {
		String expr = "define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}";
		expect(expr, Int.of(144));
	}

	private void expect(String expr, Node expect) {
		assertEquals(expect, new LazyFunInterpreter().lazy(Suite.parse(expr)).get());
	}

}
