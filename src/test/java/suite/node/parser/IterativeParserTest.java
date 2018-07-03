package suite.node.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.pp.PrettyPrinter;
import suite.os.FileUtil;
import suite.os.LogUtil;
import suite.streamlet.FunUtil.Source;

public class IterativeParserTest {

	private IterativeParser iterativeParser = new IterativeParser(TermOp.values());

	@Test
	public void testParseChar() {
		test("97", "+'a'");
	}

	@Test
	public void testParseNil() {
		test("()");
	}

	@Test
	public void testParseSpacedOperator() {
		test("!, a");
		test("a, b", "a   ,   b");
	}

	@Test
	public void testParseBraces() {
		test("a {b}");
		test("a b {c}");
	}

	@Test
	public void testParseQuotes() {
		test("''''");
		test("'`' (0 - ())", "`0 -`");
		test("'`' (() - ())", "`-`");
	}

	@Test
	public void testParseColons() {
		test("a:b c:d ():e f:() g", "a:b c:d :e f: g");
		test("cmp/() {0}", "cmp/ {0}");
	}

	@Test
	public void testParseException() {
		try {
			var node = iterativeParser.parse("(a");
			System.out.println(node);
			assertNotNull(Tree.decompose(node).getLeft());
			throw new AssertionError();
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testParseExpression() {
		test("a, b :- 1 + 2 * (3 + 4) / 5 / 6 + 7 #");
	}

	@Test
	public void testParsePredicate() {
		test("length ('_', '.r') '.l1' :- length '.r' '.l0', sum '.l1' '.l0' 1");
	}

	@Test
	public void testParseAuto() {
		var in = FileUtil.read("src/main/ll/auto.sl");
		var node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	@Test
	public void testParseFile() {
		var in = FileUtil.read("src/main/ll/fc/fc.sl");
		var node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	@Test
	public void testParsePerformance() {
		var in = FileUtil.read("src/main/fl/STANDARD.slf");
		Source<Boolean> test = () -> {
			for (var i = 0; i < 20; i++)
				iterativeParser.parse(in);
			return true;
		};
		test.source(); // warm-up
		LogUtil.duration("parse", test);
	}

	private void test(String s) {
		test(s, s);
	}

	private void test(String sx, String s0) {
		var s1 = Formatter.dump(iterativeParser.parse(s0));
		assertEquals(sx, s1);
	}

}
