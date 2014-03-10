package suite.lp.doer;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;

public class Specializer {

	public Node specialize(Node node) {
		node = node.finalNode();

		if (node instanceof Reference) {
			Reference ref = (Reference) node;
			node = Atom.create(Generalizer.variablePrefix + ref.getId());
		} else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = tree.getLeft(), right = tree.getRight();
			Node left1 = specialize(left), right1 = specialize(right);
			if (left != left1 || right != right1)
				node = Tree.create(tree.getOperator(), left1, right1);
		}

		return node;
	}

}
