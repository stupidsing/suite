package suite.node.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.inspect.Inspect;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.parser.RecursiveFactorizer.FNode;
import suite.node.parser.RecursiveFactorizer.FNodeType;
import suite.node.parser.RecursiveFactorizer.FTerminal;
import suite.node.parser.RecursiveFactorizer.FTree;
import suite.node.util.TreeRewriter;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Nodify;
import suite.util.To;

public class RecursiveFactorizerTest {

	private RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());

	@Test
	public void testParseUnparse() throws IOException {
		String s0 = FileUtil.read("src/main/ll/auto.sl").trim();
		FNode fn = recursiveFactorizer.parse(s0);
		String sx = recursiveFactorizer.unparse(fn);
		assertEquals(s0, sx);
	}

	@Test
	public void testDirectReplace() throws IOException {
		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		FNode fnx = transform(fn0);
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

	@Test
	public void testRefactorRewrite() throws IOException {
		Reference r[] = new Reference[64];

		for (int i = 0; i < r.length; i++)
			r[i] = new Reference();

		Fun<Boolean, Node> fun = b -> {
			Source<Node> g = To.source(r);
			Node head = terminalNode(g, b ? "ic-compile1" : "ic-compile0");
			Node n0;
			if (!b)
				n0 = g.source();
			else
				n0 = operatorNode(TermOp.TUPLE_, Arrays.asList(g.source(), newTerminalNode(" "), newTerminalNode(".type")));
			Node n1 = tupleNode(g, g.source(), n0);
			Node n2 = tupleNode(g, g.source(), n1);
			return tupleNode(g, head, n2);
		};

		Node nodefr = fun.apply(false);
		Node nodeto = fun.apply(true);
		TreeRewriter tr = new TreeRewriter(nodefr, nodeto);

		Nodify nodify = new Nodify(new Inspect());

		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		Node node0 = nodify.nodify(FNode.class, fn0);
		Node nodex = tr.rewrite(node0);
		FNode fnx = nodify.unnodify(FNode.class, nodex);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
		assertFalse(sx.contains("ic-compile0"));
	}

	private Node tupleNode(Source<Node> g, Node n0, Node n1) {
		return operatorNode(TermOp.TUPLE_, Arrays.asList(n0, terminalNode(g, ""), n1));
	}

	private Node operatorNode(Operator operator, List<Node> nodes) {
		return treeNode(FNodeType.OPERATOR, new Str(operator.toString()), nodes);
	}

	private Node terminalNode(Source<Node> g, String s) {
		Node r0 = g.source();
		Node r1 = g.source();
		return terminalNode(r0, termNode(s), r1);
	}

	private Node newTerminalNode(String s) {
		return terminalNode(termNode(""), termNode(s), termNode(""));
	}

	private Node terminalNode(Node n0, Node n1, Node n2) {
		return treeNode(FNodeType.TERMINAL, Atom.of("null"), Arrays.asList(n0, n1, n2));
	}

	private Node treeNode(FNodeType type, Node name, List<Node> nodes) {
		Dict dict = new Dict();
		dict.map.put(Atom.of("type"), Reference.of(Atom.of(type.toString())));
		dict.map.put(Atom.of("name"), Reference.of(name));
		dict.map.put(Atom.of("fns"), Reference.of(Tree.of(TermOp.OR____, nodes)));
		return Tree.of(TermOp.COLON_, Atom.of("suite.node.parser.RecursiveFactorizer$FTree"), dict);
	}

	private Node termNode(String s) {
		Dict dict = new Dict();
		dict.map.put(Atom.of("chars"), Reference.of(new Str(s)));
		return Tree.of(TermOp.COLON_, Atom.of("suite.node.parser.RecursiveFactorizer$FTerminal"), dict);
	}

}
