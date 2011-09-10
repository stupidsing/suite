package org.suite.doer;

import java.util.HashMap;
import java.util.Map;

import org.suite.kb.RuleSet.Rule;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;

public class Cloner {

	private Map<Reference, Reference> references = new HashMap<Reference, Reference>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.getHead()), clone(rule.getTail()));
	}

	public Node clone(Node node) {
		if (node instanceof Reference) {
			node = node.finalNode();

			if (node instanceof Reference) {
				Reference oldRef = (Reference) node;
				node = references.get(oldRef);

				if (node == null) {
					Reference newRef = new Reference();
					node = newRef;
					references.put(oldRef, newRef);
				}
			}
		}

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = clone(tree.getLeft());
			Node right = clone(tree.getRight());
			node = new Tree(tree.getOperator(), left, right);
		}

		return node;
	}

}
