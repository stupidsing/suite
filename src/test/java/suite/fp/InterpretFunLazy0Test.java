package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;

public class InterpretFunLazy0Test {

	@Test
	public void testFibonacci() {
		var expr0 = "define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) ~ fib {12}";
		assertEquals(Int.of(144), new InterpretFunLazy0().lazy(Suite.parse(expr0)).get());

		var expr1 = "" //
				+ "define take := (i => l => if (0 < i) then (take {i - 1} {snd {l}}) else (fst {l})) ~ " //
				+ "define zip-add := (l0 => l1 => (fst {l0} + fst {l1}, zip-add {snd {l0}} {snd {l1}})) ~ " //
				+ "define fib := 0, 1, zip-add {fib} {snd {fib}} ~ " //
				+ "take {12} {fib}";
		assertEquals(Int.of(144), new InterpretFunLazy0().lazy(Suite.parse(expr1)).get());
	}

}
