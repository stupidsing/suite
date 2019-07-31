package suite.node.util;

import java.util.HashSet;
import java.util.Set;

import primal.adt.IdentityKey;
import suite.node.Node;
import suite.node.Tree;

public class Cyclic {

	private Set<IdentityKey<Node>> checkedNodes = new HashSet<>();
	private Set<IdentityKey<Node>> checkingNodes = new HashSet<>();

	public boolean isCyclic(Node node) {
		IdentityKey<Node> idHashNode = IdentityKey.of(node);
		boolean isCyclic;

		if (!checkedNodes.contains(idHashNode)) {
			if (checkingNodes.add(idHashNode)) {
				var tree = Tree.decompose(node);
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
