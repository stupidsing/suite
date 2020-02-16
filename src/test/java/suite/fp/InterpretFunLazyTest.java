package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;

public class InterpretFunLazyTest {

	@Test
	public void testDecons() {
		expect("let l := (1; 3;) ~ if (l = `$a; $b;`) then b else error ()", Int.of(3));
	}

	@Test
	public void testFibonacci() {
		expect("define fib := (n => if (1 < n) then (fib_{n - 1} + fib_{n - 2}) else n) ~ fib_{12}", Int.of(144));
	}

	@Test
	public void testLets() {
		expect("lets (a := b + 2 # b := 1 #) ~ a", Int.of(3));
		expect("lets (b := 1 # a := b + 2 #) ~ a", Int.of(3));
	}

	@Test
	public void testNestedFunction() {
		expect("define inc := (define inc0 := (x => x + 1) ~ inc0) ~ inc_{3}", Suite.parse("4"));
	}

	private void expect(String expr, Node expected) {
		expect0(true, expr, expected);
		expect0(false, expr, expected);
		expect1(expr, expected);
	}

	private void expect0(boolean isLazyify, String expr, Node expected) {
		var interpreter = new InterpretFunEager();
		interpreter.setLazyify(isLazyify);
		assertEquals(expected, interpreter.eager(Suite.parse(expr)));
	}

	private void expect1(String expr, Node expected) {
		var interpreter = new InterpretFunLazy();
		assertEquals(expected, interpreter.lazy(Suite.parse(expr)).get());
	}

}
