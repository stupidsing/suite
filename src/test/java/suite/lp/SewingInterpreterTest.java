package suite.lp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.sewing.SewingProver;

public class SewingInterpreterTest {

	@Test
	public void test() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "yes");

		SewingProver pi = new SewingProver(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(pi.compile(Suite.parse("yes")).apply(pc));
		assertTrue(pi.compile(Suite.parse("fail; yes")).apply(pc));
		assertTrue(pi.compile(Suite.parse("yes; yes")).apply(pc));
		assertFalse(pi.compile(Suite.parse("yes, fail")).apply(pc));

		assertFalse(pi.compile(Suite.parse("!, fail; yes")).apply(pc));

		assertTrue(pi.compile(Suite.parse("(.v = 1; .v = 2), .v = 2")).apply(pc));
		assertFalse(pi.compile(Suite.parse("once (.v = 1; .v = 2), .v = 2")).apply(pc));

		assertFalse(pi.compile(Suite.parse("not yes")).apply(pc));
		assertTrue(pi.compile(Suite.parse("not fail")).apply(pc));

		assertTrue(pi.compile(Suite.parse(".p = yes, .p")).apply(pc));
	}

	@Test
	public void testCut() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "a :- b");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b :- !, fail");

		SewingProver pi = new SewingProver(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(pi.compile(Suite.parse("a")).apply(pc));
	}

	@Test
	public void testEnv() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "a :- b .a, b .b");
		Suite.addRule(rs, "b 1");

		SewingProver pi = new SewingProver(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(pi.compile(Suite.parse("a")).apply(pc));
	}

}
