package suite.lp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;

public class ProveInterpreterTest {

	@Test
	public void test() {
		ProveInterpreter pi = new ProveInterpreter(Suite.createRuleSet());
		assertTrue(pi.compile(Suite.parse("yes; fail")).source());
		assertFalse(pi.compile(Suite.parse("yes, fail")).source());
	}

}
