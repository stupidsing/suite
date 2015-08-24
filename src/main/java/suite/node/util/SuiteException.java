package suite.node.util;

import suite.node.Node;
import suite.node.io.Formatter;

public class SuiteException extends RuntimeException {

	private static final long serialVersionUID = 1l;

	private Node node;

	public SuiteException(Node node) {
		this(node, null);
	}

	public SuiteException(Node node, String details) {
		super(Formatter.display(node) + (details != null ? "\n" + details : ""));
		this.node = node;
	}

	public Node getNode() {
		return node;
	}

}
