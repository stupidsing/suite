package suite.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.IterativeParser;
import suite.node.io.PrettyPrinter;
import suite.node.io.TermOp;
import suite.util.To;

public class IterativeParserTest {

	private IterativeParser iterativeParser = new IterativeParser(TermOp.values());

	@Test
	public void testParseNil() {
		test("()");
	}

	@Test
	public void testParseSpacedOperator() {
		assertEquals("a, b", Formatter.dump(iterativeParser.parse("a   ,   b")));
	}

	@Test
	public void testParseBraces() {
		test("a {b}");
	}

	@Test
	public void testParseTupleWithBraces() {
		test("a b {c}");
	}

	@Test
	public void testParseColon() {
		test("a:b c:d :e f");
	}

	@Test
	public void testParseException() {
		try {
			Node node = iterativeParser.parse("(a");
			System.out.println(node);
			assertNotNull(Tree.decompose(node).getLeft());
			throw new AssertionError();
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testParseExpression() {
		test("a, b :- 1 + 2 * (3 + 4) / 5 / 6 + 7");
	}

	@Test
	public void testParseQuoted() {
		test("''''");
	}

	@Test
	public void testParsePredicate() {
		test("length (_, .r) .l1 :- length .r .l0, sum .l1 .l0 1");
	}

	@Test
	public void testParseAuto() throws IOException {
		String in = To.string(new File("src/main/resources/auto.sl"));
		Node node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	@Test
	public void testParseFile() throws IOException {
		String in = To.string(new File("src/main/resources/fc.sl"));
		Node node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	private void test(String s) {
		String s1 = Formatter.dump(iterativeParser.parse(s));
		assertEquals(s, s1);
	}

}
