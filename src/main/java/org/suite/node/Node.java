package org.suite.node;

import org.suite.doer.Comparer;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	public int compareTo(Node other) {
		return Comparer.comparer.compare(this, other);
	}

}
