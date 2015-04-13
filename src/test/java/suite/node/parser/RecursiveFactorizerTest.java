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
import suite.node.parser.RecursiveFactorizer.FR;
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
		FR fr = recursiveFactorizer.parse(s0);
		String sx = recursiveFactorizer.unparse(fr);
		assertEquals(s0, sx);
	}

	@Test
	public void testDirectReplace() throws IOException {
		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FR fr0 = recursiveFactorizer.parse(s0);
		FR frx = transform(fr0);
		String sx = recursiveFactorizer.unparse(frx);
		System.out.println(sx);
	}

	private FR transform(FR fr) {
		return new FR(fr.pre, transform(fr.node), fr.post);
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
				fnx = new FTree(ft.type, ft.name, fns, ft.spaces);
			} else
				fnx = fn0;
		return fnx;
	}

	@Test
	public void testRefactorRewrite() throws IOException {
		Source<Node[]> source = () -> {
			Reference r[] = new Reference[64];

			for (int i = 0; i < r.length; i++)
				r[i] = new Reference();

			Reference refto = new Reference();

			Reference reffr = new Reference() {
				public void bound(Node node) {
					super.bound(node);
					refto.bound(node);
				}

				public void unbound() {
					refto.unbound();
					super.unbound();
				}
			};

			Fun<String, Fun<Boolean, Node>> fun = hs -> b -> {
				Source<Node> g = To.source(r);
				Node head = terminalNode(hs);
				Node n0 = !b ? reffr : operatorNode(TermOp.TUPLE_, Arrays.asList(refto, terminalNode(" "), terminalNode(".type")));
				Node n1 = tupleNode(g, g.source(), n0);
				Node n2 = tupleNode(g, g.source(), n1);
				return tupleNode(g, head, n2);
			};

			return new Node[] { fun.apply("ic-compile0").apply(false), fun.apply("ic-compile1").apply(true) };
		};

		TreeRewriter tr = new TreeRewriter();
		Nodify nodify = new Nodify(new Inspect());

		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FR fr0 = recursiveFactorizer.parse(s0);
		FNode fn0 = fr0.node;
		Node node0 = nodify.nodify(FNode.class, fn0);
		Node nodex = tr.rewrite(source, node0);
		FNode fnx = nodify.unnodify(FNode.class, nodex);
		FR frx = new FR(fr0.pre, fnx, fr0.post);
		String sx = recursiveFactorizer.unparse(frx);
		System.out.println(sx);
		assertFalse(sx.contains("ic-compile0"));
	}

	private Node tupleNode(Source<Node> g, Node n0, Node n1) {
		return operatorNode(g, TermOp.TUPLE_, Arrays.asList(n0, terminalNode(""), n1));
	}

	private Node operatorNode(Source<Node> g, Operator operator, List<Node> nodes) {
		return treeNode(g, FNodeType.OPERATOR, new Str(operator.toString()), nodes);
	}

	private Node operatorNode(Operator operator, List<Node> nodes) {
		return treeNode(FNodeType.OPERATOR, new Str(operator.toString()), nodes);
	}

	private Node treeNode(FNodeType type, Node name, List<Node> nodes) {
		Str s = new Str("");
		return treeNode(type, name, nodes, Tree.of(TermOp.OR____, Arrays.asList(s, s, s, s)));
	}

	private Node treeNode(Source<Node> g, FNodeType type, Node name, List<Node> nodes) {
		return treeNode(type, name, nodes, Reference.of(g.source()));
	}

	private Node treeNode(FNodeType type, Node name, List<Node> nodes, Node spaces) {
		Dict dict = new Dict();
		dict.map.put(Atom.of("type"), Reference.of(Atom.of(type.toString())));
		dict.map.put(Atom.of("name"), Reference.of(name));
		dict.map.put(Atom.of("fns"), Reference.of(Tree.of(TermOp.OR____, nodes)));
		dict.map.put(Atom.of("spaces"), Reference.of(spaces));
		return Tree.of(TermOp.COLON_, Atom.of("suite.node.parser.RecursiveFactorizer$FTree"), dict);
	}

	private Node terminalNode(String s) {
		Dict dict = new Dict();
		dict.map.put(Atom.of("chars"), Reference.of(new Str(s)));
		return Tree.of(TermOp.COLON_, Atom.of("suite.node.parser.RecursiveFactorizer$FTerminal"), dict);
	}

}
