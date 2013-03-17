package org.suite.node;

import java.util.List;

import org.parser.Operator;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.TermParser.TermOp;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	public static Atom atom(String name) {
		return Atom.create(name);
	}

	public static Int num(int integer) {
		return Int.create(integer);
	}

	public static Reference ref() {
		return new Reference();
	}

	public static Node list(Operator operator, Node... nodes) {
		Node result;

		if (nodes.length > 0) {
			int i = nodes.length;
			result = nodes[--i];

			while (--i >= 0)
				result = Tree.create(operator, nodes[i], result);
		} else
			result = Atom.nil;

		return result;
	}

	public static Node list(Operator operator, List<Node> nodes) {
		return list(operator, nodes.toArray(new Node[nodes.size()]));
	}

	public static Node list(List<Node> nodes) {
		return list(TermOp.TUPLE_, nodes);
	}

	public static Node list(Node... nodes) {
		return list(TermOp.TUPLE_, nodes);
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
