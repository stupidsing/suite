package suite.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.adt.Pair;
import suite.streamlet.Read;
import suite.util.Util;

public class StronglyConnectedComponentsTest {

	@Test
	public void test() {
		StronglyConnectedComponents<String> scc = new StronglyConnectedComponents<>(DirectedGraph.of(Util.set( //
				Pair.of("a", "b") //
				, Pair.of("b", "c") //
				, Pair.of("b", "e") //
				, Pair.of("b", "f") //
				, Pair.of("c", "d") //
				, Pair.of("c", "g") //
				, Pair.of("d", "c") //
				, Pair.of("d", "h") //
				, Pair.of("e", "a") //
				, Pair.of("e", "f") //
				, Pair.of("f", "g") //
				, Pair.of("g", "f") //
				, Pair.of("h", "d") //
				, Pair.of("h", "g") //
		)));

		assertEquals(3, scc.components.size());
		assertTrue(Read.from(scc.components).isAny(c -> c.equals(Util.set("a", "b", "e"))));
		assertTrue(Read.from(scc.components).isAny(c -> c.equals(Util.set("c", "d", "h"))));
		assertTrue(Read.from(scc.components).isAny(c -> c.equals(Util.set("f", "g"))));
	}

}
