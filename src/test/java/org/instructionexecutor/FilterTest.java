package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;
import org.suite.FunCompilerConfig;
import org.suite.Suite;

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

		String program1 = Suite.applyFilter(program);

		FunCompilerConfig fcc = Suite.fcc(program1, true);
		fcc.setIn(reader);
		fcc.setOut(writer);

		Suite.evaluateFun(fcc);
		String result = writer.toString();
		return result;
	}

}
