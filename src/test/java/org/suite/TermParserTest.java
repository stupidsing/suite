package org.suite;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.suite.node.Tree;
import org.util.LogUtil;

public class TermParserTest {

	@Test
	public void testParse() {
		LogUtil.initLog4j();
		assertNotNull(Tree.decompose(SuiteUtil.parse("!, a")).getLeft());
	}

}
