package suite.fp;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.node.io.Formatter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CircularProgrammingTest {

	@Test
	public void test() throws IOException {
		var fp = "" //
				+ "data (Binary-tree :t) over :t as Tree (Binary-tree :t, Binary-tree :t) ~ \n " //
				+ "data (Binary-tree :t) over :t as Leaf :t ~ \n " //
				+ "let mintree := t => \n " //
				+ "    lets ( \n " //
				+ "        mintree1 := \n " //
				+ "            case \n " //
				+ "            || `Leaf $n` => (n, Leaf n1) \n " //
				+ "            || `Tree ($l, $r)` => \n " //
				+ "                let `$minl, $l1` := mintree1_{l} ~ \n " //
				+ "                let `$minr, $r1` := mintree1_{r} ~ \n " //
				+ "                lesser_{minl}_{minr}, Tree (l1, r1) \n " //
				+ "            || anything => error () \n " //
				+ "        # \n " //
				+ "        n1 := first_{mintree1_{t}} # \n " //
				+ "        t1 := second_{mintree1_{t}} # \n " //
				+ "    ) ~ \n " //
				+ "    t1 \n " //
				+ "~ \n " //
				+ "mintree_{Tree (Tree (Leaf 1, Leaf 2), Tree (Leaf 3, Tree (Leaf 4, Leaf 5)))} \n ";
		var result = Suite.evaluateFun(fp, true);
		assertNotNull(result);
		assertEquals("Tree, (Tree, (Leaf, 1), Leaf, 1), Tree, (Leaf, 1), Tree, (Leaf, 1), Leaf, 1", Formatter.dump(result));
	}

}
