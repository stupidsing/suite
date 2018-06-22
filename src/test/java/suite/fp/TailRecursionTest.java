package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;

public class TailRecursionTest {

	@Test
	public void testTailRecursion() {
		Suite.noLibraries(() -> {
			var fp0 = "" //
					+ "define dec := n => if (1 < n) then (dec {n - 1}) else 0 \n" //
					+ ">> \n" //
					+ "dec {65536}";
			assertEquals(Int.of(0), Suite.evaluateFun(fp0, false));

			var fp1 = "" //
					+ "define sum := n => s => if (0 < n) then (sum {n - 1} {s + n}) else s >> \n" //
					+ "sum {16384} {0}";
			assertEquals(Int.of((1 + 16384) * 16384 / 2), Suite.evaluateFun(fp1, false));

			var fp2 = "" //
					+ "define or := \n" //
					+ "  x => y => if x then true else y \n" //
					+ ">> \n" //
					+ "define fold-left := fun => init => \n" //
					+ "  case \n" //
					+ "  || `$h; $t` => fold-left {fun} {fun {init} {h}} {t} \n" //
					+ "  || anything => init \n" //
					+ ">> \n" //
					+ "fold-left {`+`} {0} {0; 1; 2; 3; 4; 5; 6; 7; 8; 9;}";
			assertEquals(Int.of(45), Suite.evaluateFun(fp2, false));
			return true;
		});
	}

}
