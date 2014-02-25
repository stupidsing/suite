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
import suite.node.io.TermParser.TermOp;
import suite.util.To;

public class IterativeParserTest {

	private IterativeParser iterativeParser = new IterativeParser(TermOp.values());

	@Test
	public void testParse0() {
		test("()");
	}

	@Test
	public void testParse1() {
		assertEquals("a, b", Formatter.dump(iterativeParser.parse("a   , b")));
	}

	@Test
	public void testParse2() {
		test("a {b}");
	}

	@Test
	public void testParse3() {
		test("a b {c}");
	}

	@Test
	public void testParse4() {
		test("a:b c:d e");
	}

	@Test
	public void testParse5() {
		try {
			Node node = iterativeParser.parse("(a");
			System.out.println(node);
			assertNotNull(Tree.decompose(node).getLeft());
			throw new AssertionError();
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testParse6() {
		test("a, b :- 1 + 2 * (3 + 4) / 5 / 6 + 7");
	}

	@Test
	public void testParse7() {
		test("''''");
	}

	@Test
	public void testParseFile() throws IOException {
		String in = To.string(new File("src/main/resources/fc.sl"));
		Node node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node).getLeft());
	}

	private void test(String s) {
		assertEquals(s, Formatter.dump(iterativeParser.parse(s)));
	}

}
