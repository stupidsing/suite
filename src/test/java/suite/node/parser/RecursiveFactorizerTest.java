package suite.node.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.Verbs.ReadString;
import primal.Verbs.Take;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.parser.Operator;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.BaseOp;
import suite.node.io.TermOp;
import suite.node.parser.FactorizeResult.FNode;
import suite.node.parser.FactorizeResult.FPair;
import suite.node.parser.FactorizeResult.FTerminal;
import suite.node.parser.FactorizeResult.FTree;
import suite.node.util.Rewrite;
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.util.Nodify;
import suite.util.To;

public class RecursiveFactorizerTest {

	private Nodify nodify = Singleton.me.nodify;
	private RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values);
	private Rewrite rw = new Rewrite();

	@Test
	public void testParseUnparse() {
		var s0 = ReadString.from("src/main/ll/auto.sl").trim();
		var fr = recursiveFactorizer.parse(s0);
		var sx = fr.unparse();
		assertEquals(s0, sx);
	}

	@Test
	public void testPrologComments() {
		var recursiveFactorizer = new RecursiveFactorizer(TermOp.values);
		var rf = recursiveFactorizer.parse("" //
				+ "-- comment\n" //
				+ "0\n");
		assertTrue(0 < rf.pre.size());
		// system.out.println(Dump.object(rf));
	}

	@Test
	public void testDirectReplace() {
		var s0 = ReadString.from("src/main/ll/ic/ic.sl").trim();
		var fr0 = recursiveFactorizer.parse(s0);
		var frx = transform(fr0);
		var sx = frx.unparse();
		System.out.println(sx);
	}

	private FactorizeResult transform(FactorizeResult fr) {
		return new FactorizeResult(fr.pre, transform(fr.node), fr.post);
	}

	private FNode transform(FNode fn0) {
		var from = new FTerminal(To.chars("ic-compile-better-option"));
		var to = new FTerminal(To.chars("ic-new-compile-better-option"));
		Iterate<FNode> fun = fn_ -> fn_.equals(from) ? to : null;
		return transform(fn0, fun);
	}

	private FNode transform(FNode fn0, Iterate<FNode> fun) {
		var fnx = fun.apply(fn0);
		if (fnx == null)
			if (fn0 instanceof FTree ft) {
				var pairs = Read //
						.from(ft.pairs) //
						.map(pair -> new FPair(transform(pair.node, fun), pair.chars)) //
						.toList();
				fnx = new FTree(ft.name, pairs);
			} else
				fnx = fn0;
		return fnx;
	}

	@Test
	public void testRefactorRewrite0() {
		var pred0 = "ic-compile-better-option";
		var predx = "ic-new-compile-better-option";
		var sx = rewriteNewArgument(pred0, predx, ".type", ReadString.from("src/main/ll/ic/ic.sl").trim());

		System.out.println(sx);
		assertFalse(sx.contains(pred0));
	}

	private String rewriteNewArgument(String pred0, String predx, String newArgument, String s0) {
		Source<Node[]> source = () -> {
			var r = new Reference[64];

			for (var i = 0; i < r.length; i++)
				r[i] = new Reference();

			Fun<String, Fun<Boolean, Node>> fun = hs -> b -> {
				Source<Node> g = Take.from(r);
				var head = terminalNode(hs);
				var n0 = !b ? g.g() : operatorNode(TermOp.TUPLE_, List.of(g.g(), terminalNode(" "), terminalNode(newArgument)));
				var n1 = operatorNode(g, TermOp.TUPLE_, g.g(), n0);
				return operatorNode(g, TermOp.TUPLE_, head, n1);
			};

			return new Node[] { fun.apply(pred0).apply(false), fun.apply(predx).apply(true) };
		};

		var fr0 = recursiveFactorizer.parse(s0);
		var fn0 = fr0.node;
		var node0 = nodify.nodify(FNode.class, fn0);
		var nodex = rw.rewrite(source, node0);
		var fnx = nodify.unnodify(FNode.class, nodex);
		var frx = new FactorizeResult(fr0.pre, fnx, fr0.post);
		var sx = frx.unparse();
		return sx;
	}

	private Node operatorNode(Operator op, List<Node> nodes) {
		var s = new Str("");
		var name = new Str(op.toString());
		return treeNode(() -> s, name, nodes);
	}

	private Node operatorNode(Source<Node> g, Operator op, Node n0, Node n1) {
		var name = new Str(op.toString());
		var nodes = List.of(n0, terminalNode(op.name_().trim()), n1);
		return treeNode(g, name, nodes);
	}

	private Node treeNode(Source<Node> g, Node name, List<Node> nodes) {
		var pairs = Read.from(nodes).map(node -> pairNode(node, g.g())).toList();

		var map = new HashMap<Node, Reference>();
		map.put(Atom.of("name"), Reference.of(name));
		map.put(Atom.of("pairs"), Reference.of(TreeUtil.buildUp(BaseOp.OR____, pairs)));

		return Tree.of(TermOp.COLON_, Atom.of(FTree.class.getName()), Dict.of(map));
	}

	private Node pairNode(Node n0, Node n1) {
		var map = new HashMap<Node, Reference>();
		map.put(Atom.of("node"), Reference.of(n0));
		map.put(Atom.of("chars"), Reference.of(n1));
		return Dict.of(map);
	}

	private Node terminalNode(String s) {
		var map = new HashMap<Node, Reference>();
		map.put(Atom.of("chars"), Reference.of(new Str(s)));
		return Tree.of(TermOp.COLON_, Atom.of(FTerminal.class.getName()), Dict.of(map));
	}

	@Test
	public void testRefactorRewrite1() {
		var pred0 = "ic-compile-better-option .0 .1 .2";
		var predx = "ic-new-compile-better-option .0 .1 .2 .type";
		var sx = recursiveFactorizer.rewrite(pred0, predx, ReadString.from("src/main/ll/ic/ic.sl").trim());

		System.out.println(sx);
		assertFalse(sx.contains(pred0));
	}

}
