package suite.node.util;

import java.util.HashSet;
import java.util.Set;

import suite.node.IdHashNode;
import suite.node.Node;
import suite.node.Tree;

public class Cyclic {

	private Set<IdHashNode> checkedNodes = new HashSet<>();
	private Set<IdHashNode> checkingNodes = new HashSet<>();

	public boolean isCyclic(Node node) {
		node = node.finalNode();
		IdHashNode idHashNode = new IdHashNode(node);
		boolean isCyclic;

		if (!checkedNodes.contains(idHashNode)) {
			if (checkingNodes.add(idHashNode)) {
				Tree tree = Tree.decompose(node);
				isCyclic = tree != null && (isCyclic(tree.getLeft()) || isCyclic(tree.getRight()));
				checkingNodes.remove(idHashNode);
			} else
				isCyclic = true;

			checkedNodes.add(idHashNode);
		} else
			isCyclic = false;

		return isCyclic;
	}

}
