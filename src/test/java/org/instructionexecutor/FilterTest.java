package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;
import org.suite.FunCompilerConfig;
import org.suite.Suite;
import org.suite.node.Node;

public class FilterTest {

	@Test
	public void testDirect() {
		assertEquals("abcdef", eval("c => c", "abcdef"));
	}

	@Test
	public void testMap() {
		assertEquals("bcdefg", eval("map {`+ 1`}", "abcdef"));
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

		try {
			Suite.evaluateFunIo(fcc, reader, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return writer.toString();
	}

}
