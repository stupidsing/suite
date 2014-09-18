package suite.lp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;

public class ProveInterpreterTest {

	@Test
	public void test() {
		ProveInterpreter pi = new ProveInterpreter(Suite.createRuleSet());
		assertTrue(pi.compile(Suite.parse("yes")).source());
		assertTrue(pi.compile(Suite.parse("fail; yes")).source());
		assertTrue(pi.compile(Suite.parse("yes; yes")).source());
		assertFalse(pi.compile(Suite.parse("!, fail; yes")).source());
		assertFalse(pi.compile(Suite.parse("yes, fail")).source());
		assertTrue(pi.compile(Suite.parse("(.v = 1; .v = 2), .v = 2")).source());
		assertFalse(pi.compile(Suite.parse("once (.v = 1; .v = 2), .v = 2")).source());
	}

}
