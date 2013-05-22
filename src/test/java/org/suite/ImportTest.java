package org.suite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;

public class ImportTest {

	@Test
	public void testImport() throws IOException {
		RuleSet rs = RuleSetUtil.create();
		Suite.importResource(rs, "auto.sl");
		assertTrue(Suite.proveThis(rs, "list"));
		assertTrue(Suite.proveThis(rs, "list repeat"));
	}

}
