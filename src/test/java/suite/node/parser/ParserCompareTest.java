package suite.node.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import primal.Verbs.ReadString;
import primal.parser.Operator;
import suite.node.io.TermOp;

public class ParserCompareTest {

	private Operator[] operators = TermOp.values();
	private IterativeParser iterativeParser = new IterativeParser(operators);
	private RecursiveParser recursiveParser = new RecursiveParser(operators);

	@Test
	public void test() {
		test(" test");
		test("1 : 2 * (3 - 4)");
		test(ReadString.from("src/main/ll/auto.sl"));
	}

	private void test(String in) {
		var n0 = iterativeParser.parse(in);
		var n1 = recursiveParser.parse(in);
		assertEquals(n0, n1);
	}

}
