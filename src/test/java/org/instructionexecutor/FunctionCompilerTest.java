package org.instructionexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.node.Tree;

public class FunctionCompilerTest {

	private static final String and = "" //
			+ "and = (x => y => x ? y | false) >> \n";

	private static final String concat = "" //
			+ "concat = (lol => \n" //
			+ "    is-tree {lol} \n" //
			+ "    ? ( \n" //
			+ "        l = head {lol} >> \n" //
			+ "        r = tail {lol} >> ( \n" //
			+ "            is-tree {l} \n" //
			+ "            ? cons {head {l}} {concat {cons {tail {l}} {r}}} \n" //
			+ "            | concat {r} \n" //
			+ "        ) \n" //
			+ "    ) \n" //
			+ "    | () \n" //
			+ ") >> \n";

	private static final String contains = "" //
			+ "contains = (e => \n" //
			+ "    join {fold {or}} {map {e1 => e1 = e}} \n" //
			+ ") >> \n";

	private static final String fold = "" //
			+ "fold = (f => l => \n" //
			+ "    h = head {l} >> \n" //
			+ "    t = tail {l} >> \n" //
			+ "    is-tree {t} ? f {h} {fold {f} {t}} | h \n" //
			+ ") >> \n";

	private static final String join = "" //
			+ "join = (f => g => x => f {g {x}}) >> \n";

	private static final String map = "" //
			+ "map = (f => l => \n" //
			+ "    is-tree {l} \n" //
			+ "    ? cons {f {head {l}}} {map {f} {tail {l}}} \n" //
			+ "    | () \n" //
			+ ") >> \n";

	private static final String or = "" //
			+ "or = (x => y => x ? true | y) >> \n";

	@Test
	public void testClosure() {
		assertEquals(Int.create(7), eval("" //
				+ "add = (p => q => p + q) >> add {3} {4}"));
		assertEquals(Int.create(20), eval("" //
				+ "p = (n => n + 1) >> \n" //
				+ "q = (n => p {n} * 2) >> \n" //
				+ "q {9}"));
	}

	@Test
	public void testConcat() {
		assertEquals(SuiteUtil.parse("1:2:3:4:5:6:"), eval("" //
				+ concat //
				+ "concat {(1:2:):(3:4:):(5:6:):}"));
	}

	@Test
	public void testContains() {
		assertEquals(Atom.create("true"), eval("" //
				+ join + fold + or + map + contains //
				+ "contains {9} {7:8:9:10:11:}"));
		assertEquals(Atom.create("false"), eval("" //
				+ join + fold + or + map + contains //
				+ "contains {12} {7:8:9:10:11:}"));
	}

	@Test
	public void testJoin() {
		assertEquals(Int.create(19), eval("" //
				+ join //
				+ "p = (n => n + 1) >> \n" //
				+ "q = (n => n * 2) >> \n" //
				+ "r = (join {p} {q}) >> \n" //
				+ "r {9}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.create(89), eval("" //
				+ "fib = (n => \n" //
				+ "    n > 1 \n" //
				+ "    ? fib {n - 1} + fib {n - 2} \n" //
				+ "    | 1 \n" //
				+ ") >> \n" //
				+ "fib {10}"));
	}

	@Test
	public void testFold() {
		assertEquals(Int.create(324), eval("" //
				+ fold //
				+ "fold {a => b => a * b} {2:3:6:9:}"));
	}

	@Test
	public void testSwitch() {
		assertEquals(new Str("C"), eval("" //
				+ "switch = (p => \n" //
				+ "    p = 1 ? \"A\" | \n" //
				+ "    p = 2 ? \"B\" | \n" //
				+ "    p = 3 ? \"C\" | \n" //
				+ "    \"D\" \n" //
				+ ") >> \n" //
				+ "switch {3}"));
	}

	@Test
	public void testIf() {
		assertEquals(Int.create(0), eval("3 > 4 ? 1 | 0"));
		assertEquals(Int.create(1), eval("3 = 3 ? 1 | 0"));
	}

	@Test
	public void testMap() {
		assertEquals(SuiteUtil.parse("5:6:7:"), eval("" //
				+ map //
				+ "map {n => n + 2} {3:4:5:}"));
	}

	@Test
	public void testOperator() {
		assertEquals(Atom.create("true"), eval("" //
				+ and + or //
				+ "and {1 = 1} {or {1 = 0} {1 = 1}}"));
	}

	@Test
	public void testRange() {
		assertEquals(SuiteUtil.parse("2:5:8:11:"), eval("" //
				+ "range = (i => j => inc => \n" //
				+ "    i != j \n" //
				+ "    ? cons {i} {range {i + inc} {j} {inc}} \n" //
				+ "    | () \n" //
				+ ") >> \n" //
				+ "range {2} {14} {3}"));
	}

	@Test
	public void testSys() {
		assertNotNull(Tree.decompose(eval("cons {1} {2:}")));
		assertEquals(Int.create(1), eval("head {1:2:3:}"));
		assertNotNull(Tree.decompose(eval("tail {1:2:3:}")));
	}

	private static Node eval(String f) {
		return SuiteUtil.evaluateFunctional(f);
	}

}
