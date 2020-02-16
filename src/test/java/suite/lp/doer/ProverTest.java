package suite.lp.doer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static primal.statics.Fail.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.kb.RuleSet;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.SewingProverBuilder2;

public class ProverTest {

	@Test
	public void testAppend() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "app () .l .l");
		Suite.addRule(rs, "app (.h, .r) .l (.h, .r1) :- app .r .l .r1");

		assertTrue(test(rs, "app (a, b, c,) (d, e,) (a, b, c, d, e,)"));
	}

	@Test
	public void testCut() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "a :- !, fail");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "yes");

		assertFalse(test(rs, "a"));
		assertFalse(Suite.proveLogic(rs, "cut.begin .c, (nl, cut.end .c, fail; yes)"));
		assertTrue(Suite.proveLogic(rs, "(cut.begin .c, nl, cut.end .c, fail); yes"));
	}

	@Test
	public void testFindAll() {
		var rs = Suite.newRuleSet();
		assertTrue(test(rs, "find.all .v (.v = a; .v = b; .v = c) .results, .results = (a, b, c,)"));
	}

	@Test
	public void testIsCyclic() {
		var rs = Suite.newRuleSet();
		assertFalse(test(rs, ".a = (a, b, c,), is.cyclic .a"));
		assertTrue(test(rs, ".a = (a, b, .a, c,), is.cyclic .a"));
	}

	@Test
	public void testLet() {
		var rs = Suite.newRuleSet();
		assertTrue(test(rs, "let 7 (2 * 3 + 1)"));
		assertFalse(test(rs, "let 7 (2 * 3 - 1)"));
	}

	@Test
	public void testMember() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "mem ((.e, _), .e)");
		Suite.addRule(rs, "mem ((_, .remains), .e) :- mem (.remains, .e)");

		assertTrue(test(rs, "mem ((a, ), a)"));
		assertTrue(test(rs, "mem ((a, b, c, ), .v)"));
		assertTrue(test(rs, ".l = (1, 2, 3,), find.all .v (mem (.l, .v)) .l"));
		assertFalse(test(rs, "mem ((a, b, c, ), d)"));
	}

	@Test
	public void testNotNot() throws IOException {
		assertTrue(test(Suite.newRuleSet(), "not (not (.a = 3)), not (bound .a)"));
	}

	@Test
	public void testProve() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b");
		Suite.addRule(rs, "c");
		Suite.addRule(rs, "a b .v :- fail");
		Suite.addRule(rs, "a b c");
		Suite.addRule(rs, ".var is a man");
		Suite.addRule(rs, "q :- once (not yes; yes)");
		Suite.addRule(rs, "yes");

		assertTrue(Suite.proveLogic(rs, ""));
		assertTrue(Suite.proveLogic(rs, "a"));
		assertTrue(Suite.proveLogic(rs, "a, b"));
		assertTrue(Suite.proveLogic(rs, "a, b, c"));
		assertTrue(Suite.proveLogic(rs, "a, fail; b"));
		assertTrue(Suite.proveLogic(rs, "a b c"));
		assertTrue(Suite.proveLogic(rs, "abc is a man"));
		assertTrue(Suite.proveLogic(rs, ".v = a, .v = b; .v = c"));
		assertTrue(Suite.proveLogic(rs, "[1, 2, 3] = [1, 2, 3]"));
		assertTrue(Suite.proveLogic(rs, "q"));

		assertFalse(Suite.proveLogic(rs, "fail"));
		assertFalse(Suite.proveLogic(rs, "d"));
		assertFalse(Suite.proveLogic(rs, "a, fail"));
		assertFalse(Suite.proveLogic(rs, "fail, a"));
		assertFalse(Suite.proveLogic(rs, "a b d"));
		assertFalse(Suite.proveLogic(rs, "a = b"));
		assertFalse(Suite.proveLogic(rs, ".v = a, .v = b"));
	}

	@Test
	public void testSystemPredicates() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "mem ((.e, _), .e)");
		Suite.addRule(rs, "mem ((_, .remains), .e) :- mem (.remains, .e)");

		assertTrue(test(rs, ".l = (1, 2,), find.all .v (mem (.l, .v)) .l"));
	}

	@Test
	public void testTree() {
		var rs = Suite.newRuleSet();
		assertFalse(test(rs, "tree .t0 a ':' b, tree .t1 a ':' b, same .t0 .t1"));
		assertTrue(test(rs, "intern.tree .t0 a ':' b, intern.tree .t1 a ':' b, same .t0 .t1"));
	}

	@Test
	public void testWrite() {
		var rs = Suite.newRuleSet();
		assertTrue(test(rs, "write (1 + 2 * 3), nl"));
		assertTrue(test(rs, "write \"Don\"\"t forget%0A4 Jun 1989\", nl"));
	}

	private boolean test(RuleSet rs, String lp) {
		var pc = new ProverCfg();
		var b0 = Suite.proveLogic(new InterpretedProverBuilder(pc), rs, lp);
		var b1 = Suite.proveLogic(new SewingProverBuilder2(pc), rs, lp);
		if (b0 == b1)
			return b0;
		else
			return fail("different prove result");
	}

}
