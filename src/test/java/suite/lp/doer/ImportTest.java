package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.kb.RuleSet;

public class ImportTest {

	@Test
	public void testImport() throws IOException {
		RuleSet rs = Suite.newRuleSet(List.of("auto.sl"));
		assertTrue(Suite.proveLogic(rs, "list"));
		assertTrue(Suite.proveLogic(rs, "list repeat"));
	}

	@Test
	public void testImportFunCompiler() throws IOException {
		RuleSet rs = Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl"));
		System.out.println(rs.getRules().size());
	}

}
