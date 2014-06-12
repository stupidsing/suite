package suite.node.util;

import java.util.HashSet;
import java.util.Set;

import suite.node.Node;
import suite.node.Tree;

public class Cyclic {

	private Set<IdHashKey> checkedNodes = new HashSet<>();
	private Set<IdHashKey> checkingNodes = new HashSet<>();

	public boolean isCyclic(Node node) {
		node = node.finalNode();
		IdHashKey idHashNode = new IdHashKey(node);
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
