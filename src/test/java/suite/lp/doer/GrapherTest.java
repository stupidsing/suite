package suite.lp.doer;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.lp.search.SewingProverBuilder2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrapherTest {

	@Test
	public void testBind() {
		assertTrue(prove(".u = hello, .v = hello, graph.bind .u .v"));
		assertTrue(prove(".u = 1 .u:.u, .v = 1 .v:.v, graph.bind .u .v"));
		assertTrue(prove(".u = 1 .u:.v, .v = 1 .v:.u, graph.bind .u .v"));
		assertFalse(prove(".u = 1 .u, .v = 2 .v, graph.bind .u .v"));
		assertFalse(prove(".u = 1 .u, .v = .v 1, graph.bind .u .v"));
		assertFalse(prove(".u = .u 1, .v = .v 2, graph.bind .u .v"));
	}

	private boolean prove(String lp) {
		var rs = Suite.newRuleSet();
		return Suite.proveLogic(new SewingProverBuilder2(), rs, lp);
	}

}
