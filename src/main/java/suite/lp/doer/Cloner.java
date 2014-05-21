package suite.lp.doer;

import java.util.IdentityHashMap;
import java.util.Map;

import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;

public class Cloner {

	private Map<Reference, Reference> references = new IdentityHashMap<>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.getHead()), clone(rule.getTail()));
	}

	public Node clone(Node node) {
		Tree tree = Tree.of(null, null, node);
		cloneRight(tree);
		return tree.getRight();
	}

	private void cloneRight(Tree tree) {
		while (true) {
			Node right = tree.getRight().finalNode();

			if (right instanceof Reference)
				right = getNewReference((Reference) right);

			if (right instanceof Tree) {
				Tree rightTree = (Tree) right;
				rightTree = Tree.of(rightTree.getOperator(), clone(rightTree.getLeft()), rightTree.getRight());
				Tree.forceSetRight(tree, rightTree);
				tree = rightTree;
				continue;
			}

			Tree.forceSetRight(tree, right);
			break;
		}
	}

	public Node cloneOld(Node node) {
		node = node.finalNode();

		if (node instanceof Reference)
			node = getNewReference((Reference) node);

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = tree.getLeft(), right = tree.getRight();
			Node left1 = clone(left), right1 = clone(right);
			if (left != left1 || right != right1)
				node = Tree.of(tree.getOperator(), left1, right1);
		}

		return node;
	}

	private Node getNewReference(Reference oldReference) {
		Node node = references.get(oldReference);

		if (node == null) {
			Reference newReference = new Reference();
			node = newReference;
			references.put(oldReference, newReference);
		}

		return node;
	}

}
