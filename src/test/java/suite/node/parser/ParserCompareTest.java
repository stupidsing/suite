package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.node.Node;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FileUtil;

public class ParserCompareTest {

	private Operator operators[] = TermOp.values();
	private IterativeParser iterativeParser = new IterativeParser(operators);
	private RecursiveParser recursiveParser = new RecursiveParser(operators);

	@Test
	public void testParseExpressionCompare() {
		String in = "1 : 2 * (3 - 4)";
		Node n0 = iterativeParser.parse(in);
		Node n1 = recursiveParser.parse(in);
		assertEquals(n0, n1);
	}

	@Test
	public void testParseFileCompare() throws IOException {
		String in = FileUtil.read("src/main/ll/auto.sl");
		Node n0 = iterativeParser.parse(in);
		Node n1 = recursiveParser.parse(in);
		assertEquals(n0, n1);
	}

}
