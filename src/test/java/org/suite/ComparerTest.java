package org.suite;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.suite.doer.Comparer;
import org.suite.node.Int;
import org.suite.node.Reference;

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
		assertTrue(comparer.compare(SuiteUtil.parse("1 = 2"), Int.create(3)) > 0);
		assertTrue(comparer.compare(SuiteUtil.parse("a"), Int.create(3)) > 0);
		assertTrue(comparer.compare(SuiteUtil.parse("b"), new Reference()) > 0);
	}

}
