package suite.node;

import java.util.List;

import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermParser.TermOp;
import suite.node.util.Comparer;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	public static Reference ref() {
		return new Reference();
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

	@Override
	public int compareTo(Node other) {
		return Comparer.comparer.compare(this, other);
	}

	@Override
	public String toString() {
		return Formatter.dump(this);
	}

}
