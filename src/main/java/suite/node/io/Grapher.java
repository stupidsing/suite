package suite.node.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.adt.Pair;
import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Rewriter.NodeHead;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.ReadType;
import suite.streamlet.As;
import suite.streamlet.Read;

/**
 * Converts a node into graph representation. The nodes link to other nodes via
 * an integer key.
 */
public class Grapher {

	private List<NodeGraph> ngs = new ArrayList<>();

	private static class IntPair {
		private int t0;
		private int t1;

		private static IntPair of(int t0, int t1) {
			IntPair pair = new IntPair();
			pair.t0 = t0;
			pair.t1 = t1;
			return pair;
		}
	}

	private class NodeGraph extends NodeHead {
		private List<IntPair> children;

		private NodeGraph(Node terminal) {
			this(ReadType.TERM, terminal, null, Collections.emptyList());
		}

		private NodeGraph(ReadType type, Node terminal, Operator op, List<IntPair> children) {
			super(type, terminal, op);
			this.children = children;
		}
	}

	public int graph(Node node) {
		return graph0(new HashMap<>(), node);
	}

	private int graph0(Map<IdentityKey<Node>, Integer> ids, Node node) {
		IdentityKey<Node> key = IdentityKey.of(node.finalNode());
		Integer id = ids.get(key);

		if (id == null) {
			ids.put(key, id = ngs.size());
			ngs.add(null);

			NodeRead nr = NodeRead.of(node);

			List<IntPair> childrenx = Read.from(nr.children) //
					.map(p -> IntPair.of(graph0(ids, p.t0), graph0(ids, p.t1))) //
					.toList();

			ngs.set(id, new NodeGraph(nr.type, nr.terminal, nr.op, childrenx));
		}

		return id;
	}

	public Node ungraph(int id) {
		int size = ngs.size();

		List<Node> nodes = Read.from(ngs).map(ng -> {
			switch (ng.type) {
			case DICT:
				return new Dict();
			case LIST:
				Node n = Atom.NIL;
				for (int i = 0; i < ng.children.size(); i++)
					n = Tree.of(ng.op, null, n);
				return n;
			case TERM:
				return ng.terminal;
			case TREE:
				return Tree.of(ng.op, null, null);
			case TUPLE:
				return new Tuple(new ArrayList<>(ng.children.size()));
			default:
				throw new RuntimeException();
			}
		}).toList();

		for (int i = 0; i < size; i++) {
			NodeGraph ng = ngs.get(i);
			Node node = nodes.get(i);
			List<Pair<Node, Node>> children = Read.from(ng.children).map(p -> Pair.of(nodes.get(p.t0), nodes.get(p.t1))).toList();

			switch (ng.type) {
			case DICT:
				((Dict) node).map.putAll(Read.from(children).toMap(p -> p.t0, p -> Reference.of(p.t1)));
				break;
			case LIST:
				for (Pair<Node, Node> child : children) {
					Tree tree = (Tree) node;
					Tree.forceSetLeft(tree, child.t1);
					node = tree.getRight();
				}
				break;
			case TERM:
				break;
			case TREE:
				Tree tree = (Tree) node;
				Tree.forceSetLeft(tree, children.get(0).t1);
				Tree.forceSetRight(tree, children.get(1).t1);
				break;
			case TUPLE:
				List<Node> list = ((Tuple) node).nodes;
				list.addAll(Read.from(children).map(p -> p.t1).toList());
			}
		}

		return nodes.get(id);
	}

	public void generalize() {
		ngs = Read.from(ngs) //
				.map(ng -> {
					NodeGraph ng1;
					if (ng.type == ReadType.TERM) {
						Node node = ng.terminal.finalNode();
						if (node instanceof Atom && ((Atom) node).name.startsWith(SewingGeneralizer.variablePrefix))
							ng1 = new NodeGraph(new Reference());
						else
							ng1 = ng;
					} else
						ng1 = ng;
					return ng1;
				}) //
				.toList();
	}

	public void specialize() {
		int counter[] = new int[] { 0 };
		ngs = Read.from(ngs) //
				.map(ng -> {
					NodeGraph ng1;
					if (ng.type == ReadType.TERM) {
						Node node = ng.terminal.finalNode();
						if (node instanceof Reference)
							ng1 = new NodeGraph(Atom.of(SewingGeneralizer.variablePrefix + counter[0]++));
						else
							ng1 = ng;
					} else
						ng1 = ng;
					return ng1;
				}) //
				.toList();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (NodeGraph ng : ngs) {
			String s;
			switch (ng.type) {
			case DICT:
				s = Read.from(ng.children) //
						.map(p -> p.t0 + ":" + p.t1 + ", ") //
						.collect(As.joined("dict(", ", ", ")"));
				break;
			case LIST:
				s = Read.from(ng.children) //
						.map(p -> p.t1 + ", ") //
						.collect(As.joined("list(", ng.op.getName().trim() + " ", ")"));
				break;
			case TERM:
				s = Formatter.dump(ng.terminal);
				break;
			case TREE:
				s = "tree(" + ng.children.get(0).t1 + ng.op.getName() + ng.children.get(1).t1 + ")";
				break;
			case TUPLE:
				s = Read.from(ng.children) //
						.map(p -> p.t1 + ", ") //
						.collect(As.joined("tuple(", ", ", ")"));
				break;
			default:
				throw new RuntimeException();
			}
			sb.append(s + "\n");
		}

		return sb.toString();
	}

}
