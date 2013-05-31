package org.instructionexecutor;

import org.junit.Test;
import org.suite.FunCompilerConfig;
import org.suite.Suite;
import org.suite.node.Node;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class FilterTest {

	@Test
	public void testDirect() {
		assertEquals("abcdef", eval("c => c", "abcdef"));
	}

	@Test
	public void testSplit() {
		assertEquals("abc\ndef\nghi", eval("tail . concat . map {cons {10}} . split {32}", "abc def ghi"));
	}

	private static String eval(String program, String in) {
		StringReader reader = new StringReader(in);
		StringWriter writer = new StringWriter();

		Node node = Suite.applyFilter(Suite.parse(program));

		FunCompilerConfig fcc = Suite.fcc(node, true);
		fcc.setIn(reader);
		fcc.setOut(writer);

		Suite.evaluateFun(fcc);
		return writer.toString();
	}

}
