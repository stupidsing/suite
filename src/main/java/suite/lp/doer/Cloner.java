package suite.lp.doer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.lp.kb.Rule;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.util.IdentityKey;
import suite.streamlet.Read;

public class Cloner {

	private Map<IdentityKey, Node> clonedNodes = new HashMap<>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.head), clone(rule.tail));
	}

	public Node clone(Node node) {
		Tree tree = Tree.of(null, null, node);
		cloneRight(tree);
		return tree.getRight();
	}

	private void cloneRight(Tree tree) {
		while (tree != null) {
			Tree nextTree = null;
			Node right = tree.getRight().finalNode();
			IdentityKey key = new IdentityKey(right);
			Node right1 = clonedNodes.get(key);
			Tree rt;

			if (right1 == null) {
				if (right instanceof Dict) {
					Map<Node, Reference> map = new HashMap<>();
					((Dict) right).map.entrySet().forEach(e -> map.put(clone(e.getKey()), Reference.of(clone(e.getValue()))));
					right1 = new Dict(map);
				} else if (right instanceof Reference)
					right1 = new Reference();
				else if (right1 instanceof Tuple) {
					List<Node> nodes = ((Tuple) right1).nodes;
					right1 = new Tuple(Read.from(nodes).map(this::clone).toList());
				} else if ((rt = Tree.decompose(right)) != null)
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
		return clonedNodes.computeIfAbsent(new IdentityKey(node.finalNode()), key -> {
			Node node_ = key.node;

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
