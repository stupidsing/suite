package suite.lp.doer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.kb.RuleSet;
import suite.lp.search.SewingProverBuilder;

public class GrapherTest {

	@Test
	public void testBind() {
		assertTrue(prove(".u = hello, .v = hello, graph.bind .u .v"));
		assertTrue(prove(".u = 1 .u, .v = 1 .v, graph.bind .u .v"));
		assertFalse(prove(".u = 1 .u, .v = 2 .v, graph.bind .u .v"));
		assertFalse(prove(".u = 1 .u, .v = .v 1, graph.bind .u .v"));
	}

	private boolean prove(String lp) {
		RuleSet rs = Suite.createRuleSet();
		return Suite.proveLogic(new SewingProverBuilder(), rs, lp);
	}

}
