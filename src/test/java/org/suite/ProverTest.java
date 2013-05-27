package org.suite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;

public class ProverTest {

	@Test
	public void testAppend() {
		RuleSet rs = RuleSetUtil.create();
		Suite.addRule(rs, "app () .l .l");
		Suite.addRule(rs, "app (.h, .r) .l (.h, .r1) :- app .r .l .r1");

		assertTrue(Suite.proveThis(rs, "app (a, b, c,) (d, e,) (a, b, c, d, e,)"));
	}

	@Test
	public void testCut() {
		RuleSet rs = RuleSetUtil.create();
		Suite.addRule(rs, "a :- !, fail");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "yes");
		assertFalse(Suite.proveThis(rs, "a"));

		assertFalse(Suite.proveThis(rs, "cut.begin .c, (dump ALT:.c, nl, cut.end .c, fail; yes)"));
		assertTrue(Suite.proveThis(rs, "(cut.begin .c, dump ALT:.c, nl, cut.end .c, fail); yes"));
	}

	@Test
	public void testFindAll() {
		assertTrue(proveThis("find.all .v (.v = a; .v = b; .v = c) .results" + ", .results = (a, b, c, )"));
	}

	@Test
	public void testMember() {
		RuleSet rs = RuleSetUtil.create();
		Suite.addRule(rs, "mem ([.e, _], .e)");
		Suite.addRule(rs, "mem ([_, .remains], .e) :- mem (.remains, .e)");

		assertTrue(Suite.proveThis(rs, "mem ([a, ], a)"));
		assertTrue(Suite.proveThis(rs, "mem ([a, b, c, ], .v)"));
		assertTrue(Suite.proveThis(rs, ".l = [1, 2, 3,], find.all .v (mem (.l, .v)) .l"));
		assertFalse(Suite.proveThis(rs, "mem ([a, b, c, ], d)"));
	}

	@Test
	public void testNotNot() throws IOException {
		assertTrue(proveThis("not not (.a = 3), not bound .a"));
	}

	@Test
	public void testProve() {
		RuleSet rs = RuleSetUtil.create();
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b");
		Suite.addRule(rs, "c");
		Suite.addRule(rs, "a b .v :- fail");
		Suite.addRule(rs, "a b c");
		Suite.addRule(rs, ".var is a man");

		assertTrue(Suite.proveThis(rs, ""));
		assertTrue(Suite.proveThis(rs, "a"));
		assertTrue(Suite.proveThis(rs, "a, b"));
		assertTrue(Suite.proveThis(rs, "a, b, c"));
		assertTrue(Suite.proveThis(rs, "a, fail; b"));
		assertTrue(Suite.proveThis(rs, "a b c"));
		assertTrue(Suite.proveThis(rs, "abc is a man"));
		assertTrue(Suite.proveThis(rs, ".v = a, .v = b; .v = c"));
		assertTrue(Suite.proveThis(rs, "[1, 2, 3] = [1, 2, 3]"));

		assertFalse(Suite.proveThis(rs, "fail"));
		assertFalse(Suite.proveThis(rs, "d"));
		assertFalse(Suite.proveThis(rs, "a, fail"));
		assertFalse(Suite.proveThis(rs, "fail, a"));
		assertFalse(Suite.proveThis(rs, "a b d"));
		assertFalse(Suite.proveThis(rs, "a = b"));
		assertFalse(Suite.proveThis(rs, ".v = a, .v = b"));
	}

	@Test
	public void testSystemPredicates() {
		RuleSet rs = RuleSetUtil.create();
		Suite.addRule(rs, "mem ([.e, _], .e)");
		Suite.addRule(rs, "mem ([_, .remains], .e) :- mem (.remains, .e)");

		assertTrue(Suite.proveThis(rs, ".l = [1, 2,], find.all .v (mem (.l, .v)) .l"));
	}

	@Test
	public void testWrite() {
		assertTrue(proveThis("write (1 + 2 * 3), nl"));
		assertTrue(proveThis("write \"Don\"\"t forget%0A4 Jun 1989\", nl"));
	}

	private boolean proveThis(String s) {
		return Suite.proveThis(RuleSetUtil.create(), s);
	}

}
