package suite.node.util;

import java.util.HashSet;
import java.util.Set;

import suite.node.IdHashNode;
import suite.node.Node;
import suite.node.Tree;

public class Cyclic {

	private Set<IdHashNode> checkedNodes = new HashSet<>();
	private Set<IdHashNode> traversedNodes = new HashSet<>();

	public boolean isCyclic(Node node) {
		node = node.finalNode();
		IdHashNode idHashNode = new IdHashNode(node);

		if (checkedNodes.add(idHashNode)) {
			if (traversedNodes.add(idHashNode))
				try {
					Tree tree = Tree.decompose(node);

					if (tree != null)
						return isCyclic(tree.getLeft()) || isCyclic(tree.getRight());
					else
						return false;
				} finally {
					traversedNodes.remove(idHashNode);
				}
			else
				return true;
		} else
			return false;
	}

}
