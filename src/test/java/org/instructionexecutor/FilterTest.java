package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;
import org.suite.Main;
import org.suite.SuiteUtil;
import org.suite.SuiteUtil.FunCompilerConfig;
import org.suite.node.Node;
import org.util.IoUtil;

public class FilterTest {

	@Test
	public void testDirect() {
		eval("abcdef", "abcdef", "c => c");
	}

	private static Node eval(String in, String out, String program) {
		byte inBytes[] = in.getBytes(IoUtil.charset);
		ByteArrayInputStream is = new ByteArrayInputStream(inBytes);
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		FunCompilerConfig config = FunCompilerConfig.create(
				Main.applyFilter(program), true);
		config.setIn(is);
		config.setOut(new PrintStream(os));

		Node result = SuiteUtil.evaluateFunctional(config);
		assertEquals(out, new String(os.toByteArray(), IoUtil.charset));
		return result;
	}

}
