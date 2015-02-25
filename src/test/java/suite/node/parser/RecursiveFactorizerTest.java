package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.node.io.TermOp;
import suite.node.parser.RecursiveFactorizer.FNode;
import suite.node.parser.RecursiveFactorizer.FTerminal;
import suite.node.parser.RecursiveFactorizer.FTree;
import suite.streamlet.Read;
import suite.util.FileUtil;
import suite.util.To;

public class RecursiveFactorizerTest {

	private RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());

	@Test
	public void testParseAuto() throws IOException {
		String s0 = FileUtil.read("src/main/ll/auto.sl").trim();
		FNode fn = recursiveFactorizer.parse(s0);
		String sx = recursiveFactorizer.unparse(fn);
		assertEquals(s0, sx);
	}

	@Test
	public void testRefactorFile() throws IOException {
		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		FNode fnx = transform(fn0);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	private FNode transform(FNode fn) {
		FTerminal from = new FTerminal(To.chars("ic-compile0"));
		FTerminal to = new FTerminal(To.chars("ic-compile1"));

		if (fn.equals(from))
			return to;
		else if (fn instanceof FTree) {
			FTree ft = (FTree) fn;
			return new FTree(ft.type, ft.name, Read.from(ft.fns).map(this::transform).toList());
		} else
			return fn;
	}

}
