package suite.node.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.node.Node;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.os.FileUtil;

public class ParserCompareTest {

	private Operator[] operators = TermOp.values();
	private IterativeParser iterativeParser = new IterativeParser(operators);
	private RecursiveParser recursiveParser = new RecursiveParser(operators);

	@Test
	public void testParseExpressionCompare() {
		test("1 : 2 * (3 - 4)");
	}

	@Test
	public void testParseFileCompare() {
		test(FileUtil.read("src/main/ll/auto.sl"));
	}

	private void test(String in) {
		Node n0 = iterativeParser.parse(in);
		Node n1 = recursiveParser.parse(in);
		assertEquals(n0, n1);
	}

}
