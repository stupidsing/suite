package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.inspect.Inspect;
import suite.node.Node;
import suite.node.io.Lister;
import suite.node.io.TermOp;
import suite.node.parser.RecursiveFactorizer.FNode;
import suite.node.parser.RecursiveFactorizer.FNodeType;
import suite.node.parser.RecursiveFactorizer.FTerminal;
import suite.node.parser.RecursiveFactorizer.FTree;
import suite.node.util.TreeRewriter;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Nodify;
import suite.util.To;
import suite.util.Util;

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
	public void testRefactorReplace() throws IOException {
		FTerminal from = new FTerminal(To.chars("ic-compile0"));
		FTerminal to = new FTerminal(To.chars("ic-compile1"));
		Fun<FNode, FNode> fun = fn_ -> fn_.equals(from) ? to : null;

		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		FNode fnx = transform(fn0, fun);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	@Test
	public void testRefactorReplace1() throws IOException {
		Fun<FNode, FNode> fun = fn_ -> {
			FNode fn01 = gets(fn_, 0, 1);
			boolean b = true //
			&& isTerm(fn01, "ic-compile0");
			return b ? new FTree(FNodeType.OPERATOR, TermOp.TUPLE_.name, Arrays.asList(gets(fn_, 0, 0))) : null;
		};

		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		FNode fnx = transform(fn0, fun);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	@Test
	public void testRefactorRewrite() throws IOException {
		Nodify nodify = new Nodify(new Inspect());
		Node nodefr = nodify.nodify(FNode.class, recursiveFactorizer.parse("ic-compile0"));
		Node nodeto = nodify.nodify(FNode.class, recursiveFactorizer.parse("ic-compile1"));
		System.out.println("FROM " + new Lister().list(nodefr));
		System.out.println("TO__ " + new Lister().list(nodeto));
		TreeRewriter tr = new TreeRewriter(nodefr, nodeto);

		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		Node node0 = nodify.nodify(FNode.class, fn0);
		Node nodex = tr.replace(node0);
		FNode fnx = nodify.unnodify(FNode.class, nodex);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	private FNode transform(FNode fn0) {
		FTerminal from = new FTerminal(To.chars("ic-compile0"));
		FTerminal to = new FTerminal(To.chars("ic-compile1"));
		Fun<FNode, FNode> fun = fn_ -> fn_.equals(from) ? to : null;
		return transform(fn0, fun);
	}

	private FNode transform(FNode fn0, Fun<FNode, FNode> fun) {
		FNode fnx = fun.apply(fn0);
		if (fnx == null)
			if (fn0 instanceof FTree) {
				FTree ft = (FTree) fn0;
				List<FNode> fns = Read.from(ft.fns).map(fn_ -> transform(fn_, fun)).toList();
				fnx = new FTree(ft.type, ft.name, fns);
			} else
				fnx = fn0;
		return fnx;
	}

	private boolean isTerm(FNode fn, String s) {
		return fn instanceof FTerminal && Util.stringEquals(((FTerminal) fn).chars.toString(), s);
	}

	private FNode gets(FNode fn, int... ns) {
		for (int n : ns)
			fn = get(fn, n);
		return fn;
	}

	private FNode get(FNode fn, int n) {
		if (fn instanceof FTree) {
			List<FNode> fns = ((FTree) fn).fns;
			return n < fns.size() ? fns.get(n) : null;
		} else
			return null;
	}

}
