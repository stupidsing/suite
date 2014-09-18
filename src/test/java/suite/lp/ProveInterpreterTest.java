package suite.lp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;

public class ProveInterpreterTest {

	@Test
	public void test() {
		RuleSet rs = Suite.createRuleSet();
		ProveInterpreter pi = new ProveInterpreter(rs);
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
	}

}
