package org.suite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.suite.kb.RuleSet;

public class ImportTest {

	@Test
	public void testImport() throws IOException {
		RuleSet rs = new RuleSet();
		ClassLoader cl = getClass().getClassLoader();
		InputStream is = cl.getResourceAsStream("auto.sl");
		rs.importFrom(SuiteUtil.parse(is));
		assertTrue(SuiteUtil.proveThis(rs, "list"));
		assertTrue(SuiteUtil.proveThis(rs, "list repeat"));
	}

}
