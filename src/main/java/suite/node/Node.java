package suite.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermParser.TermOp;
import suite.node.util.Comparer;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	public static Node list(List<Node> nodes) {
		return list(TermOp.TUPLE_, nodes);
	}

	public static Node list(Node... nodes) {
		return list(TermOp.TUPLE_, nodes);
	}

	public static Node list(Operator operator, List<Node> nodes) {
		return list(operator, nodes.toArray(new Node[nodes.size()]));
	}

	public static Node list(Operator operator, Node... nodes) {
		Node result;

		if (nodes.length > 0) {
			int i = nodes.length;
			result = nodes[--i];

			while (--i >= 0)
				result = Tree.create(operator, nodes[i], result);
		} else
			result = Atom.NIL;

		return result;
	}

	public static Reference ref() {
		return new Reference();
	}

	public static Iterable<Node> iter(Node node) {
		return iter(node, TermOp.AND___);
	}

	public static Iterable<Node> iter(final Node node0, final Operator operator) {
		final Iterator<Node> iterator = new Iterator<Node>() {
			private Tree tree = Tree.decompose(node0, operator);

			public boolean hasNext() {
				return tree != null;
			}

			public Node next() {
				Node next = tree.getLeft();
				tree = Tree.decompose(tree.getRight(), operator);
				return next;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};

		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return iterator;
			}
		};
	}

	public static Node[] tupleToArray(Node node, int n) {
		List<Node> results = new ArrayList<>(n);
		Tree tree;

		for (int i = 1; i < n; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				results.add(tree.getLeft());
				node = tree.getRight();
			} else
				throw new RuntimeException("Not enough parameters");

		results.add(node);
		return results.toArray(new Node[results.size()]);
	}

	public static List<Node> tupleToList(Node node) {
		List<Node> rs = new ArrayList<>();
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			rs.add(tree.getLeft());
			node = tree.getRight();
		}

		rs.add(node);
		return rs;
	}

	@Override
	public int compareTo(Node other) {
		return Comparer.comparer.compare(this, other);
	}

	@Override
	public String toString() {
		return Formatter.dump(this);
	}

}
