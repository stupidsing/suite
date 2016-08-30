package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

public class EagerFunInterpreterTest {

	@Test
	public void testDecons() {
		expect("let l := (1; 3;) >> if (l = `$a; $b;`) then b else error", Int.of(3));
	}

	@Test
	public void testFibonacci() {
		expect("define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}", Int.of(144));
	}

	@Test
	public void testLazy() {
		expect("define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}", Int.of(144));
	}

	@Test
	public void testLets() {
		expect("lets (a := b + 2 # b := 1 #) >> a", Int.of(3));
		expect("lets (b := 1 # a := b + 2 #) >> a", Int.of(3));
	}

	@Test
	public void testUsing() {
		expect("using source STANDARD >> and {true} {true}", Atom.TRUE);
		expect("using source STANDARD >> log {1234}", Int.of(1234));
	}

	private void expect(String expr, Node expect) {
		expect(true, expr, expect);
		expect(false, expr, expect);
	}

	private void expect(boolean isLazyify, String expr, Node expect) {
		EagerFunInterpreter interpreter = new EagerFunInterpreter();
		interpreter.setLazyify(isLazyify);
		assertEquals(expect, new EagerFunInterpreter().eager(Suite.parse(expr)));
	}

}
