package org.suite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.kb.RuleSet;

public class TreeRbTest {

	@Test
	public void test() throws IOException {
		RuleSet rs = new RuleSet();
		SuiteUtil.importResource(rs, "auto.sl");
		SuiteUtil.importResource(rs, "tree-rb.sl");

		assertTrue(SuiteUtil.proveThis(rs, "" //
				+ "rb-add-list (6, 7, 8, 9, 10, 1, 2, 3, 4, 5,) ()/.t \n" //
				+ ", rb-get .t 8" //
				+ ", rb-member .t 4"));
	}

}
