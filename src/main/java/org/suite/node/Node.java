package org.suite.node;

import org.suite.doer.Comparer;
import org.suite.doer.Formatter;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
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
