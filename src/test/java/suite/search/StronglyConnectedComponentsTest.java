package suite.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.adt.Pair;

public class StronglyConnectedComponentsTest {

	@Test
	public void test0() {
		var scc = new StronglyConnectedComponents<>(DirectedGraph.of(Set.of( //
				Pair.of("a", "b"), //
				Pair.of("b", "c"), //
				Pair.of("c", "d"), //
				Pair.of("d", "e"), //
				Pair.of("e", "f"), //
				Pair.of("f", "g"))));

		var components = Read.from(scc.components);

		assertEquals(7, components.size());
	}

	@Test
	public void test1() {
		var scc = new StronglyConnectedComponents<>(DirectedGraph.of(Set.of( //
				Pair.of("a", "b"), //
				Pair.of("b", "c"), //
				Pair.of("b", "e"), //
				Pair.of("b", "f"), //
				Pair.of("c", "d"), //
				Pair.of("c", "g"), //
				Pair.of("d", "c"), //
				Pair.of("d", "h"), //
				Pair.of("e", "a"), //
				Pair.of("e", "f"), //
				Pair.of("f", "g"), //
				Pair.of("g", "f"), //
				Pair.of("h", "d"), //
				Pair.of("h", "g"))));

		var components = Read.from(scc.components);

		assertEquals(3, components.size());
		assertTrue(components.isAny(c -> c.equals(Set.of("a", "b", "e"))));
		assertTrue(components.isAny(c -> c.equals(Set.of("c", "d", "h"))));
		assertTrue(components.isAny(c -> c.equals(Set.of("f", "g"))));
	}

}
