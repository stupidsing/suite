package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;
import org.suite.FunCompilerConfig;
import org.suite.Main;
import org.suite.SuiteUtil;
import org.suite.node.Node;

public class FilterTest {

	@Test
	public void testDirect() {
		eval("abcdef", "abcdef", "c => c");
	}

	@Test
	public void testSplit() {
		eval("abc def ghi", "abc\ndef\nghi",
				"tail . concat . map {cons {10}} . split {32}");
	}

	private static Node eval(String in, String out, String program) {
		StringReader is = new StringReader(in);
		StringWriter os = new StringWriter();

		String program1 = Main.applyFilter(program);

		FunCompilerConfig config = SuiteUtil.fcc(program1, true);
		config.setIn(is);
		config.setOut(os);

		Node result = SuiteUtil.evaluateFun(config);
		assertEquals(out, os.toString());
		return result;
	}

}
