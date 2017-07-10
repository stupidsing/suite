package suite.node.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.parser.FactorizeResult.FNode;
import suite.node.parser.FactorizeResult.FPair;
import suite.node.parser.FactorizeResult.FTerminal;
import suite.node.parser.FactorizeResult.FTree;
import suite.node.util.Singleton;
import suite.node.util.TreeRewriter;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Nodify;
import suite.util.To;

public class RecursiveFactorizerTest {

	private Nodify nodify = Singleton.me.getNodify();
	private RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());

	@Test
	public void testParseUnparse() {
		String s0 = FileUtil.read("src/main/ll/auto.sl").trim();
		FactorizeResult fr = recursiveFactorizer.parse(s0);
		String sx = fr.unparse();
		assertEquals(s0, sx);
	}

	@Test
	public void testPrologComments() {
		RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
		FactorizeResult rf = recursiveFactorizer.parse("" //
				+ "-- comment\n" //
				+ "0\n");
		assertTrue(0 < rf.pre.size());
		// system.out.println(Dump.object(rf));
	}

	@Test
	public void testDirectReplace() {
		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FactorizeResult fr0 = recursiveFactorizer.parse(s0);
		FactorizeResult frx = transform(fr0);
		String sx = frx.unparse();
		System.out.println(sx);
	}

	private FactorizeResult transform(FactorizeResult fr) {
		return new FactorizeResult(fr.pre, transform(fr.node), fr.post);
	}

	private FNode transform(FNode fn0) {
		FTerminal from = new FTerminal(To.chars("ic-compile-better-option"));
		FTerminal to = new FTerminal(To.chars("ic-new-compile-better-option"));
		Fun<FNode, FNode> fun = fn_ -> fn_.equals(from) ? to : null;
		return transform(fn0, fun);
	}

	private FNode transform(FNode fn0, Fun<FNode, FNode> fun) {
		FNode fnx = fun.apply(fn0);
		if (fnx == null)
			if (fn0 instanceof FTree) {
				FTree ft = (FTree) fn0;
				List<FPair> pairs = Read.from(ft.pairs) //
						.map(pair -> new FPair(transform(pair.node, fun), pair.chars)) //
						.toList();
				fnx = new FTree(ft.name, pairs);
			} else
				fnx = fn0;
		return fnx;
	}

	@Test
	public void testRefactorRewrite0() {
		String pred0 = "ic-compile-better-option";
		String predx = "ic-new-compile-better-option";
		String sx = rewriteNewArgument(pred0, predx, ".type", FileUtil.read("src/main/ll/ic/ic.sl").trim());

		System.out.println(sx);
		assertFalse(sx.contains(pred0));
	}

	private String rewriteNewArgument(String pred0, String predx, String newArgument, String s0) {
		Source<Node[]> source = () -> {
			Reference[] r = new Reference[64];

			for (int i = 0; i < r.length; i++)
				r[i] = new Reference();

			Fun<String, Fun<Boolean, Node>> fun = hs -> b -> {
				Source<Node> g = To.source(r);
				Node head = terminalNode(hs);
				Node n0 = !b ? g.source()
						: operatorNode(TermOp.TUPLE_, Arrays.asList(g.source(), terminalNode(" "), terminalNode(newArgument)));
				Node n1 = operatorNode(g, TermOp.TUPLE_, g.source(), n0);
				return operatorNode(g, TermOp.TUPLE_, head, n1);
			};

			return new Node[] { fun.apply(pred0).apply(false), fun.apply(predx).apply(true) };
		};

		TreeRewriter tr = new TreeRewriter();

		FactorizeResult fr0 = recursiveFactorizer.parse(s0);
		FNode fn0 = fr0.node;
		Node node0 = nodify.nodify(FNode.class, fn0);
		Node nodex = tr.rewrite(source, node0);
		FNode fnx = nodify.unnodify(FNode.class, nodex);
		FactorizeResult frx = new FactorizeResult(fr0.pre, fnx, fr0.post);
		String sx = frx.unparse();
		return sx;
	}

	private Node operatorNode(Operator op, List<Node> nodes) {
		Str s = new Str("");
		Str name = new Str(op.toString());
		return treeNode(() -> s, name, nodes);
	}

	private Node operatorNode(Source<Node> g, TermOp op, Node n0, Node n1) {
		Str name = new Str(op.toString());
		List<Node> nodes = Arrays.asList(n0, terminalNode(op.getName().trim()), n1);
		return treeNode(g, name, nodes);
	}

	private Node treeNode(Source<Node> g, Node name, List<Node> nodes) {
		List<Node> pairs = Read.from(nodes).map(node -> pairNode(node, g.source())).toList();
		Dict dict = new Dict();
		dict.map.put(Atom.of("name"), Reference.of(name));
		dict.map.put(Atom.of("pairs"), Reference.of(Tree.of(TermOp.OR____, pairs)));
		return Tree.of(TermOp.COLON_, Atom.of(FTree.class.getName()), dict);
	}

	private Node pairNode(Node n0, Node n1) {
		Dict dict = new Dict();
		dict.map.put(Atom.of("node"), Reference.of(n0));
		dict.map.put(Atom.of("chars"), Reference.of(n1));
		return dict;
	}

	private Node terminalNode(String s) {
		Dict dict = new Dict();
		dict.map.put(Atom.of("chars"), Reference.of(new Str(s)));
		return Tree.of(TermOp.COLON_, Atom.of(FTerminal.class.getName()), dict);
	}

	@Test
	public void testRefactorRewrite1() {
		String pred0 = "ic-compile-better-option .0 .1 .2";
		String predx = "ic-new-compile-better-option .0 .1 .2 .type";
		String sx = recursiveFactorizer.rewrite(pred0, predx, FileUtil.read("src/main/ll/ic/ic.sl").trim());

		System.out.println(sx);
		assertFalse(sx.contains(pred0));
	}

}
