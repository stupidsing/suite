package suite.node.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.util.Comparer;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class Rewriter {

	public static class NodeRead {
		private Node LEFT_ = Atom.of("l");
		private Node RIGHT = Atom.of("r");

		private Comparer comparer = new Comparer();

		public final String type;
		public final Node terminal;
		public final Operator op;
		public final List<Pair<Node, Node>> children;

		public NodeRead(Node node) {
			Operator op0;
			Tree tree;
			if (node instanceof Dict) {
				Map<Node, Reference> map = ((Dict) node).map;
				type = "dict";
				terminal = null;
				op = null;
				children = Read.from(map) //
						.sort((p0, p1) -> comparer.compare(p0.t0, p1.t0)) //
						.map(p -> Pair.<Node, Node> of(p.t0, p.t1)) //
						.toList();
			} else if (Tree.isList(node, op0 = TermOp.AND___) || Tree.isList(node, op0 = TermOp.OR____)) {
				Streamlet<Node> st = Read.from(Tree.iter(node, op0));
				type = "list";
				terminal = null;
				op = op0;
				children = st.map(n -> Pair.<Node, Node> of(Atom.NIL, n)).toList();
			} else if ((tree = Tree.decompose(node)) != null) {
				Pair<Node, Node> p0 = Pair.of(LEFT_, tree.getLeft());
				Pair<Node, Node> p1 = Pair.of(RIGHT, tree.getRight());
				type = "tree";
				terminal = null;
				op = tree.getOperator();
				children = Arrays.asList(p0, p1);
			} else if (node instanceof Tuple) {
				List<Node> nodes = ((Tuple) node).nodes;
				type = "tuple";
				terminal = null;
				op = null;
				children = Read.from(nodes).map(n -> Pair.<Node, Node> of(Atom.NIL, n)).toList();
			} else {
				type = "term";
				terminal = node;
				op = null;
				children = Collections.emptyList();
			}
		}
	}

	public static class NodeWrite {
		public final Node node;

		public NodeWrite(String type, Node terminal, Operator op, List<Pair<Node, Node>> children) {
			switch (type) {
			case "dict":
				node = new Dict(Read.from(children).toMap(p -> p.t0, p -> Reference.of(p.t1)));
				break;
			case "list":
				Node n = Atom.NIL;
				for (int i = children.size() - 1; i >= 0; i--)
					n = Tree.of(op, children.get(i).t1, n);
				node = n;
				break;
			case "term":
				node = terminal;
				break;
			case "tree":
				node = Tree.of(op, children.get(0).t1, children.get(1).t1);
				break;
			case "tuple":
				node = new Tuple(Read.from(children).map(p -> p.t1).toList());
				break;
			default:
				throw new RuntimeException();
			}
		}
	}

	public static Node transform(Node node, Fun<Node, Node> fun) {
		NodeRead nr = new NodeRead(node);
		List<Pair<Node, Node>> children1 = new ArrayList<>();
		boolean isSame = true;

		for (Pair<Node, Node> pair : nr.children) {
			Node child0 = pair.t1;
			Node childx = fun.apply(child0);
			if (child0 != childx) {
				isSame = false;
				children1.add(Pair.of(pair.t0, childx));
			} else
				children1.add(pair);
		}

		if (isSame)
			return node;
		else
			return new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
	}

}
