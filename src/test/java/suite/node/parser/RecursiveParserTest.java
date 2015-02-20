package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.node.Atom;
import suite.node.io.TermOp;
import suite.node.parser.RecursiveParser.RecursiveParse;
import suite.node.util.Rewriter;
import suite.util.FileUtil;

public class RecursiveParserTest {

	private RecursiveParser recursiveParser = new RecursiveParser(TermOp.values());

	@Test
	public void testParseAuto() throws IOException {
		String in = FileUtil.read("src/main/ll/auto.sl");
		RecursiveParse rp = recursiveParser.analyze(in);
		String s1 = rp.unparse(rp.parsed);
		assertEquals(in, s1);
	}

	@Test
	public void testRefactor() throws IOException {
		Rewriter rewriter = new Rewriter(Atom.of("ic-compile0"), Atom.of("ic-compile1"));
		String in = FileUtil.read("src/main/ll/ic/ic.sl");
		String out = recursiveParser.refactor(in, rewriter::rewrite);
		System.out.println(out);
	}

}
