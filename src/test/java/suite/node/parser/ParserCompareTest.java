package suite.node.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.os.FileUtil;

public class ParserCompareTest {

	private Operator[] operators = TermOp.values();
	private IterativeParser iterativeParser = new IterativeParser(operators);
	private RecursiveParser recursiveParser = new RecursiveParser(operators);

	@Test
	public void test() {
		test(" test");
		test("1 : 2 * (3 - 4)");
		test(FileUtil.read("src/main/ll/auto.sl"));
	}

	private void test(String in) {
		var n0 = iterativeParser.parse(in);
		var n1 = recursiveParser.parse(in);
		assertEquals(n0, n1);
	}

}
