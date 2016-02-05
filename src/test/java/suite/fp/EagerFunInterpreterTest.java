package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Int;

public class EagerFunInterpreterTest {

	@Test
	public void testDecons() {
		String expr = "let l := (1; 3;) >> if (l = `$a; $b;`) then b else error";
		assertEquals(Int.of(3), new EagerFunInterpreter().eager(Suite.parse(expr)));
	}

	@Test
	public void testFibonacci() {
		String expr = "define fib := (n => if (n > 1) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}";
		assertEquals(Int.of(144), new EagerFunInterpreter().eager(Suite.parse(expr)));
	}

	@Test
	public void testLazy() {
		EagerFunInterpreter interpreter = new EagerFunInterpreter();
		interpreter.setLazyify(true);

		String expr = "define fib := (n => if (n > 1) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}";
		assertEquals(Int.of(144), interpreter.eager(Suite.parse(expr)));
	}

	@Test
	public void testUsing() {
		String expr = "using source STANDARD >> and {true} {true}";
		assertEquals(Atom.TRUE, new EagerFunInterpreter().eager(Suite.parse(expr)));
	}

}
