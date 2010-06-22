package org.fp;

import org.suite.node.Node;
import org.suite.node.Reference;

public class EvaluatableReference extends Reference {

	public boolean evaluated;

	public EvaluatableReference(Node node) {
		bound(node);
	}

}
