package suite.node;

import primal.node.Node_;
import suite.node.io.Formatter;
import suite.node.util.Comparer;

public abstract class Node extends Node_ implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	@Override
	public int compareTo(Node other) {
		return Comparer.comparer.compare(this, other);
	}

	@Override
	public String toString() {
		return Formatter.dump(finalNode());
	}

}
