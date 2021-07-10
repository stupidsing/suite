package suite.node;

import suite.node.io.Formatter;
import suite.node.util.Comparer;
import suite.object.MetadataDefaults;

public abstract class Node implements Comparable<Node>, MetadataDefaults<Node> {

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
