package org.suite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.kb.RuleSet;

public class Tree23Test {

	@Test
	public void test() throws IOException {
		RuleSet rs = new RuleSet();
		SuiteUtil.importResource(rs, "auto.sl");
		SuiteUtil.importResource(rs, "tree23.sl");

		assertTrue(SuiteUtil.proveThis(rs, "" //
				+ "tree23-add-list (6, 7, 8, 9, 10, 1, 2, 3, 4, 5,) ()/.t \n" //
				+ ", tree23-member .t 4"));
	}

}
