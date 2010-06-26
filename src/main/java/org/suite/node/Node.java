package org.suite.node;

import java.util.Collection;

import org.suite.doer.Comparer;

public class Node implements Comparable<Node> {

	public Node finalNode() {
		return this;
	}

	public void getSubNodes(Collection<Node> nodes) {
	}

	public int compareTo(Node other) {
		return Comparer.comparer.compare(this, other);
	}

}
