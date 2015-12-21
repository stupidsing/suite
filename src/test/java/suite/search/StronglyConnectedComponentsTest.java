package suite.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import suite.adt.Pair;
import suite.streamlet.Read;

public class StronglyConnectedComponentsTest {

	@Test
	public void test() {
		StronglyConnectedComponents<String> scc = new StronglyConnectedComponents<>(DirectedGraph.of(Arrays.asList( //
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
		assertTrue(Read.from(scc.components).isAny(c -> c.equals(new HashSet<>(Arrays.asList("a", "b", "e")))));
		assertTrue(Read.from(scc.components).isAny(c -> c.equals(new HashSet<>(Arrays.asList("c", "d", "h")))));
		assertTrue(Read.from(scc.components).isAny(c -> c.equals(new HashSet<>(Arrays.asList("f", "h")))));
	}

}
