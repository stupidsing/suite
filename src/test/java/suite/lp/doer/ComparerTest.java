package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Reference;
import suite.node.util.Comparer;

public class ComparerTest {

	private Comparer comparer = Comparer.comparer;

	@Test
	public void testCompareInt() {
		assertTrue(comparer.compare(Int.create(2), Int.create(3)) < 0);
		assertTrue(comparer.compare(Int.create(4), Int.create(4)) == 0);
		assertTrue(comparer.compare(Int.create(6), Int.create(5)) > 0);
	}

	@Test
	public void testOrdinality() {
		assertTrue(comparer.compare(Suite.parse("1 = 2"), Int.create(3)) > 0);
		assertTrue(comparer.compare(Suite.parse("a"), Int.create(3)) > 0);
		assertTrue(comparer.compare(Suite.parse("b"), new Reference()) > 0);
	}

}
