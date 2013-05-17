package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.node.Node;

public class IskCombinatorTest {

	private String isk = "" //
			+ "define i = (x => x) >> " //
			+ "define k = (x => y => x) >> " //
			+ "define s = (x => y => z => x {z} {y {z}}) >> ";

	@Test
	public void testSksk() {
		String sksk = "s {k} {s} {k}";
		assertEquals(SuiteUtil.parse("1"), eval(isk //
				+ "(" + sksk + ") {1} {2}"));
	}

	@Test
	public void testTf() {
		String tf = "" //
				+ "define t = k >> " //
				+ "define f = k {i} >> " //
				+ "define not_ = f {t} >> " //
				+ "define or_ = k >> " //
				+ "define and_ = f >> ";

		assertEquals(SuiteUtil.parse("1"), eval(isk + tf //
				+ "t {1} {2}"));
		assertEquals(SuiteUtil.parse("2"), eval(isk + tf //
				+ "f {1} {2}"));

		// eval(isk + tf + "t {or_} {f}") becomes t
		// eval(isk + tf + "t {or_} {f}") becomes f
	}

	private static Node eval(String f) {
		return SuiteUtil.evaluateEagerFun(f);
	}

}
