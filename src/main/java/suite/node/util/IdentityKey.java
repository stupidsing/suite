package suite.node.util;

import suite.node.Node;
import suite.util.HashCodeComparable;
import suite.util.Util;

public class IdentityKey extends HashCodeComparable<IdentityKey> {

	private Node node;

	public IdentityKey(Node node) {
		this.node = node;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(node);
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == IdentityKey.class && node == ((IdentityKey) object).node;
	}

	public Node getNode() {
		return node;
	}

}
