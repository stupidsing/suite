package suite.node.util;

import suite.node.Node;
import suite.util.Util;

public class IdHashKey {

	private Node node;

	public IdHashKey(Node node) {
		this.node = node;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(node);
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == IdHashKey.class && node == ((IdHashKey) object).node;
	}

	public Node getNode() {
		return node;
	}

}
