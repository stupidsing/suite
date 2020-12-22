package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.io.Formatter;

public class CircularProgrammingTest {

	@Test
	public void test() throws IOException {
		var fp = """
				data (Binary-tree :t) over :t as Tree (Binary-tree :t, Binary-tree :t) ~
				data (Binary-tree :t) over :t as Leaf :t ~
				let mintree := t =>
					lets (
						mintree1 :=
							case
							|| `Leaf $n` => (n, Leaf n1)
							|| `Tree ($l, $r)` =>
								let `$minl, $l1` := mintree1_{l} ~
								let `$minr, $r1` := mintree1_{r} ~
								lesser_{minl}_{minr}, Tree (l1, r1)
							|| anything => error ()
						#
						n1 := first_{mintree1_{t}} #
						t1 := second_{mintree1_{t}} #
					) ~
					t1
				~
				mintree_{Tree (Tree (Leaf 1, Leaf 2), Tree (Leaf 3, Tree (Leaf 4, Leaf 5)))}
				""";
		var result = Suite.evaluateFun(fp, true);
		assertNotNull(result);
		assertEquals("Tree, (Tree, (Leaf, 1), Leaf, 1), Tree, (Leaf, 1), Tree, (Leaf, 1), Leaf, 1",
				Formatter.dump(result));
	}

}
