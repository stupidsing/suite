package suite.node;

import suite.util.Util;

public class IdHashNode {

	private Node node;

	public IdHashNode(Node node) {
		this.node = node;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(node);
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == IdHashNode.class && node == ((IdHashNode) object).node;
	}

	public Node getNode() {
		return node;
	}

}
