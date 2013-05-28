package org.suite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.suite.kb.RuleSet;

public class ImportTest {

	@Test
	public void testImport() throws IOException {
		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl"));
		assertTrue(Suite.proveThis(rs, "list"));
		assertTrue(Suite.proveThis(rs, "list repeat"));
	}

}
