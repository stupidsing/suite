package suite.node.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Reference;

public class ComparerTest {

	private Comparer comparer = Comparer.comparer;

	@Test
	public void testCompareInt() {
		assertTrue(comparer.compare(Int.of(2), Int.of(3)) < 0);
		assertTrue(comparer.compare(Int.of(4), Int.of(4)) == 0);
		assertTrue(comparer.compare(Int.of(6), Int.of(5)) > 0);
	}

	@Test
	public void testOrdinality() {
		assertTrue(comparer.compare(Int.of(3), Suite.parse("1 = 2")) < 0);
		assertTrue(comparer.compare(Int.of(3), Suite.parse("a")) < 0);
		assertTrue(comparer.compare(new Reference(), Suite.parse("b")) < 0);
	}

}
