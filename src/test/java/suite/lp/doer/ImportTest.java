package suite.lp.doer;

import org.junit.jupiter.api.Test;
import suite.Suite;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
