package suite.node.util;

import suite.node.Node;
import suite.node.io.Formatter;

public class SuiteException extends RuntimeException {

	private static final long serialVersionUID = 1l;

	private Node node;

	public SuiteException(Node node) {
		super(Formatter.display(node));
		this.node = node;
	}

	public Node getNode() {
		return node;
	}

}
