package org.suite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.doer.Formatter;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.util.Util;

public class FormatterTest {

	@Test
	public void testFormatter() {
		assertEquals("123", Formatter.display(Int.create(123)));
		testFormat("1 + 2 * 3");
		testFormat("1 * 2 - 3");
		testFormat("1 - 2 - 3");
		testFormat("1 + 2 + 3");
		testFormat("(1 + 2) * 3");
		testFormat("a - b - c");
		testFormat("a - (b - c)");
		testFormat("(a, b) = (c, d)");
		Util.dump(Util.currentMethod(), Formatter.display(new Reference()));
	}

	private void testFormat(String s) {
		Node node = SuiteUtil.parse(s);
		assertEquals(s, Formatter.display(node));
	}

}
