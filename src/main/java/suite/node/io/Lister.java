package suite.node.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.immutable.IList;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Util;

/**
 * Lists node contents line-by-line for human-readable purpose.
 *
 * @author ywsing
 */
public class Lister {

	public static class NodeReader {
		public final String type;
		public final Node terminal;
		public final Operator op;
		public final List<Pair<String, Node>> children;

		public NodeReader(Node node) {
			Tree tree;
			if (node instanceof Dict) {
				Map<Node, Reference> map = ((Dict) node).map;
				type = "dict";
				terminal = null;
				op = null;
				children = Read.from(map).map(p -> Pair.of(p.t0.toString(), (Node) p.t1)).toList();
			} else if ((tree = Tree.decompose(node)) != null) {
				Operator operator = tree.getOperator();
				if (Arrays.asList(TermOp.AND___, TermOp.OR____).contains(operator)) {
					Streamlet<Node> st = Read.from(Tree.iter(node, operator));
					type = "list";
					terminal = null;
					op = operator;
					children = st.index((i, n) -> Pair.of(i.toString(), n)).toList();
				} else {
					Pair<String, Node> p0 = Pair.of("l", tree.getLeft());
					Pair<String, Node> p1 = Pair.of("r", tree.getRight());
					type = "tree";
					terminal = null;
					op = operator;
					children = Arrays.asList(p0, p1);
				}
			} else if (node instanceof Tuple) {
				List<Node> nodes = ((Tuple) node).nodes;
				type = "tuple";
				terminal = null;
				op = null;
				children = Read.from(nodes).index((i, n) -> Pair.of(i.toString(), n)).toList();
			} else {
				type = "term";
				terminal = node;
				op = null;
				children = Collections.emptyList();
			}
		}
	}

	public static class NodeWriter {
		public final Node node;

		public NodeWriter(String type, Node terminal, Operator op, List<Pair<String, Node>> children) {
			switch (type) {
			case "dict":
				node = new Dict(Read.from(children).toMap(p -> new Str(p.t0), p -> Reference.of(p.t1)));
				break;
			case "list":
				List<Node> list = Read.from(children).map(p -> p.t1).toList();
				Node n = Atom.NIL;
				for (int i = list.size() - 1; i >= 0; i--)
					n = Tree.of(op, list.get(i), n);
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

	public String list(Node node) {
		return leaves(node).map(this::path).collect(As.joined("\n"));
	}

	private String path(IList<Node> path) {
		return Read.from(path).map(Node::toString).reverse().collect(As.joined("."));
	}

	public Streamlet<IList<Node>> leaves(Node node) {
		return leaves(node, IList.end());
	}

	private Streamlet<IList<Node>> leaves(Node node, IList<Node> prefix) {
		NodeReader nr = new NodeReader(node);
		if (!Util.stringEquals(nr.type, "term"))
			return Read.from(nr.children).concatMap(p -> leaves(p.t1, IList.cons(new Str(p.t0), prefix)));
		else
			return Read.from(Arrays.asList(IList.cons(nr.terminal, prefix)));
	}

}
