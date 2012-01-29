package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.node.Node;

public class OlderFunctionCompilerTest {

	private static final String concatList0 = "" //
			+ "concat-list0 = split {h => t => \n" //
			+ "    if-tree {h} \n" //
			+ "        {h1 => t1 => h1:(concat-list0 {t1:t})} \n" //
			+ "        {concat-list0 {t}} \n" //
			+ "} >> \n";

	private static final String filter0 = "" //
			+ "filter0 = (fun => \n" //
			+ "    split {h => t => \n" //
			+ "        others = filter0 {fun} {t} >> \n" //
			+ "        fun {h} ? h:others | others \n" //
			+ "    } \n" //
			+ ") >> \n";

	private static final String ifTree = "" //
			+ "if-tree = (list => f1 => f2 => \n" //
			+ "    if (is-tree {list}) then ( \n" //
			+ "        f1 {head {list}} {tail {list}} \n" //
			+ "    ) \n" //
			+ "    else f2 \n" //
			+ ") >> \n";

	private static final String map0 = "" //
			+ "map0 = (fun => split {h => t => (fun {h}):(map0 {fun} {t})}) >> \n";

	private static final String split = "" //
			+ "split = (fun => list => if-tree {list} {fun} {()}) >> \n";

	@Test
	public void testConcat() {
		assertEquals(SuiteUtil.parse("1:2:3:4:5:6:"), eval("" //
				+ ifTree + split + concatList0 //
				+ "concat-list0 {(1:2:):(3:4:):(5:6:):}"));
	}

	@Test
	public void testFilter() {
		assertEquals(SuiteUtil.parse("4:6:"), eval("" //
				+ ifTree + split + filter0 //
				+ "filter0 {n => n % 2 = 0} {3:4:5:6:}"));
	}

	@Test
	public void testMap() {
		assertEquals(SuiteUtil.parse("5:6:7:"), eval("" //
				+ ifTree + split + map0 //
				+ "map0 {n => n + 2} {3:4:5:}"));
	}

	private static Node eval(String f) {
		return SuiteUtil.evaluateFunctional(f);
	}

}
