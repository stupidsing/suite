package suite.fp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.node.io.Formatter;

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
				+ "                let `$minl, $l1` := mintree1 {l} ~ \n " //
				+ "                let `$minr, $r1` := mintree1 {r} ~ \n " //
				+ "                lesser {minl} {minr}, Tree (l1, r1) \n " //
				+ "            || anything => error () \n " //
				+ "        # \n " //
				+ "        n1 := first {mintree1 {t}} # \n " //
				+ "        t1 := second {mintree1 {t}} # \n " //
				+ "    ) ~ \n " //
				+ "    t1 \n " //
				+ "~ \n " //
				+ "mintree {Tree (Tree (Leaf 1, Leaf 2), Tree (Leaf 3, Tree (Leaf 4, Leaf 5)))} \n ";
		var result = Suite.evaluateFun(fp, true);
		assertNotNull(result);
		assertEquals("Tree, (Tree, (Leaf, 1), Leaf, 1), Tree, (Leaf, 1), Tree, (Leaf, 1), Leaf, 1", Formatter.dump(result));
	}

}
