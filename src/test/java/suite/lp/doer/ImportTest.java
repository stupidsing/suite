package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import suite.lp.Suite;
import suite.lp.kb.RuleSet;

public class ImportTest {

	@Test
	public void testImport() throws IOException {
		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl"));
		assertTrue(Suite.proveLogic(rs, "list"));
		assertTrue(Suite.proveLogic(rs, "list repeat"));
	}

}
