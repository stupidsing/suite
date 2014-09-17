package suite.lp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;

public class ProveInterpreterTest {

	@Test
	public void test() {
		assertTrue(new ProveInterpreter(Suite.createRuleSet()).compile(Suite.parse("yes; fail")).source());
		assertFalse(new ProveInterpreter(Suite.createRuleSet()).compile(Suite.parse("yes, fail")).source());
	}

}
