package suite.node.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import primal.Verbs.ReadString;
import primal.fp.Funs.Source;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.pp.PrettyPrinter;
import suite.os.LogUtil;

public class IterativeParserTest {

	private IterativeParser iterativeParser = new IterativeParser(TermOp.values());

	@Test
	public void testParseAuto() {
		var in = ReadString.from("src/main/ll/auto.sl");
		var node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	@Test
	public void testParseBraces() {
		test("a_{b}");
		test("a b_{c}");
	}

	@Test
	public void testParseChar() {
		test("97", "+'a'");
	}

	@Test
	public void testParseColons() {
		test("a:b c:d ():e f:() g", "a:b c:d :e f: g");
		test("cmp/()_{0}", "cmp/_{0}");
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
	public void testParseFile() {
		var in = ReadString.from("src/main/ll/fc/fc.sl");
		var node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	@Test
	public void testParseNil() {
		test("()");
	}

	@Test
	public void testParsePerformance() {
		var in = ReadString.from("src/main/fl/STANDARD.slf");
		Source<Boolean> test = () -> {
			for (var i = 0; i < 20; i++)
				iterativeParser.parse(in);
			return true;
		};
		test.g(); // warm-up
		LogUtil.duration("parse", test);
	}

	@Test
	public void testParsePredicate() {
		test("length ('_', '.r') '.l1' :- length '.r' '.l0', sum '.l1' '.l0' 1");
	}

	@Test
	public void testParseQuotes() {
		test("''''");
		test("'`' (0 - ())", "`0 -`");
		test("'`' (() - ())", "`-`");
	}

	@Test
	public void testParseSpace() {
		test("test", " test");
	}

	@Test
	public void testParseSpacedOperator() {
		test("!, a");
		test("a, b", "a   ,   b");
	}

	private void test(String s) {
		test(s, s);
	}

	private void test(String sx, String s0) {
		var s1 = Formatter.dump(iterativeParser.parse(s0));
		assertEquals(sx, s1);
	}

}
