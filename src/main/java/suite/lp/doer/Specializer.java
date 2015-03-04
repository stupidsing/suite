package suite.lp.doer;

import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewriter;

public class Specializer {

	public Node specialize(Node node) {
		node = node.finalNode();

		if (node instanceof Reference) {
			Reference ref = (Reference) node;
			node = Atom.of(SewingGeneralizer.variablePrefix + ref.getId());
		} else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = tree.getLeft(), right = tree.getRight();
			Node left1 = specialize(left), right1 = specialize(right);
			if (left != left1 || right != right1)
				node = Tree.of(tree.getOperator(), left1, right1);
		} else
			node = Rewriter.transform(node, this::specialize);

		return node;
	}

}
