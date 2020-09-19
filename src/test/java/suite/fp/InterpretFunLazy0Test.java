package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Int;

public class InterpretFunLazy0Test {

	@Test
	public void testFibonacci() {
		var expr0 = "define fib := (n => if (1 < n) then (fib_{n - 1} + fib_{n - 2}) else n) ~ fib_{12}";
		assertEquals(Int.of(144), new InterpretFunLazy0().lazy(Suite.parse(expr0)).get());

		var expr1 = """
				define take := (i => l => if (0 < i) then (take_{i - 1}_{snd_{l}}) else (fst_{l})) ~
				define zip-add := (l0 => l1 => (fst_{l0} + fst_{l1}, zip-add_{snd_{l0}}_{snd_{l1}})) ~
				define fib := 0, 1, zip-add_{fib}_{snd_{fib}} ~
				take_{12}_{fib}
				""";
		assertEquals(Int.of(144), new InterpretFunLazy0().lazy(Suite.parse(expr1)).get());
	}

}
