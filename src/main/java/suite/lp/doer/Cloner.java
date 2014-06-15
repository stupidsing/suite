package suite.lp.doer;

import java.util.HashMap;
import java.util.Map;

import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.util.IdHashKey;

public class Cloner {

	private Map<IdHashKey, Node> clonedNodes = new HashMap<>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.getHead()), clone(rule.getTail()));
	}

	public Node clone(Node node) {
		return clonedNodes.computeIfAbsent(new IdHashKey(node), key -> {
			Tree tree = Tree.of(null, null, key.getNode());
			cloneRight(tree);
			return tree.getRight();
		});
	}

	private void cloneRight(Tree tree) {
		while (tree != null) {
			Tree nextTree = null;
			Node right = tree.getRight().finalNode();
			IdHashKey key = new IdHashKey(right);
			Node right1 = clonedNodes.get(key);
			Tree rt;

			if (right1 == null) {
				if (right instanceof Reference)
					right1 = new Reference();
				else if ((rt = Tree.decompose(right)) != null)
					right1 = nextTree = Tree.of(rt.getOperator(), clone(rt.getLeft()), rt.getRight());
				else
					right1 = right;

				clonedNodes.put(key, right1);
			}

			Tree.forceSetRight(tree, right1);
			tree = nextTree;
		}
	}

	public Node cloneOld(Node node) {
		return clonedNodes.computeIfAbsent(new IdHashKey(node.finalNode()), key -> {
			Node node_ = key.getNode();

			if (node_ instanceof Reference)
				node_ = new Reference();
			else if (node_ instanceof Tree) {
				Tree tree = (Tree) node_;
				Node left = tree.getLeft(), right = tree.getRight();
				Node left1 = clone(left), right1 = clone(right);
				if (left != left1 || right != right1)
					node_ = Tree.of(tree.getOperator(), left1, right1);
			}

			return node_;
		});
	}

}
