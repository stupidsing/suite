package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suite.node.io.TermOp;
import suite.node.parser.RecursiveFactorizer.FNode;
import suite.node.parser.RecursiveFactorizer.FTree;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
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
		FNode fnx = new Transformer().transform(fn0);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	private class Transformer {
		private CharsBuilder cb = new CharsBuilder();

		private FNode transform(FNode fn) {
			Chars from = To.chars("ic-compile0");
			Chars to = To.chars("ic-compile1");
			int p0 = cb.size();

			if (fn instanceof FTree) {
				FTree ft = (FTree) fn;
				List<FNode> fns1 = new ArrayList<>();
				for (FNode child : ft.fns)
					fns1.add(transform(child));
				Chars chars = cb.toChars().subchars(p0, cb.size());
				return new FTree(chars, ft.type, ft.name, fns1);
			} else {
				cb.append(fn.chars.replace(from, to));
				Chars chars = cb.toChars().subchars(p0, cb.size());
				return new FNode(chars);
			}
		}
	}

}
