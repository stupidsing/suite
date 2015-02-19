package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.node.io.TermOp;
import suite.node.parser.RecursiveParser.RecursiveParse;
import suite.util.FileUtil;

public class RecursiveParserTest {

	private RecursiveParser recursiveParser = new RecursiveParser(TermOp.values());

	@Test
	public void testParseAuto() throws IOException {
		String in = FileUtil.read("src/main/ll/auto.sl");
		test(in);
	}

	private void test(String s) {
		test(s, s);
	}

	private void test(String sx, String s0) {
		RecursiveParse rp = recursiveParser.analyze(s0);
		String s1 = rp.unparse(rp.parsed);
		assertEquals(sx, s1);
	}

}
