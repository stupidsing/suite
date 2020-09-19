package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Int;

public class TailRecursionTest {

	@Test
	public void testTailRecursion() {
		Suite.noLibraries(() -> {
			var fp0 = """
					define dec := n => if (1 < n) then (dec_{n - 1}) else 0
					~
					dec_{65536}
					""";
			assertEquals(Int.of(0), Suite.evaluateFun(fp0, false));

			var fp1 = """
					define sum := n => s => if (0 < n) then (sum_{n - 1}_{s + n}) else s ~
					sum_{16384}_{0}
					""";
			assertEquals(Int.of((1 + 16384) * 16384 / 2), Suite.evaluateFun(fp1, false));

			var fp2 = """
					define or :=
					  x => y => if x then true else y
					~
					define fold-left := fun => init =>
					  case
					  || `$h; $t` => fold-left_{fun}_{fun_{init}_{h}}_{t}
					  || anything => init
					~
					fold-left_{`+`}_{0}_{0; 1; 2; 3; 4; 5; 6; 7; 8; 9;}
					""";
			assertEquals(Int.of(45), Suite.evaluateFun(fp2, false));
			return true;
		});
	}

}
