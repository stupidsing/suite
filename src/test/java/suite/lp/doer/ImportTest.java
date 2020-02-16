package suite.lp.doer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import suite.Suite;

public class ImportTest {

	@Test
	public void testImport() throws IOException {
		var rs = Suite.newRuleSet(List.of("auto.sl"));
		assertTrue(Suite.proveLogic(rs, "list"));
		assertTrue(Suite.proveLogic(rs, "list repeat"));
	}

	@Test
	public void testImportFunCompiler() throws IOException {
		var rs = Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl"));
		System.out.println(rs.getRules().size());
	}

}
