package org.suite.doer;

import java.util.HashSet;
import java.util.Set;

import org.suite.node.Node;
import org.suite.node.Tree;

public class Cyclic {

	private Set<IdHashNode> nodes = new HashSet<>();

	private static class IdHashNode {
		private Node node;

		public IdHashNode(Node node) {
			this.node = node;
		}

		public int hashCode() {
			return System.identityHashCode(node);
		}

		public boolean equals(Object object) {
			if (object instanceof IdHashNode)
				return node == ((IdHashNode) object).node;
			else
				return false;
		}
	}

	public boolean isCyclic(Node node) {
		node = node.finalNode();
		IdHashNode idHashNode = new IdHashNode(node);

		if (nodes.add(idHashNode))
			try {
				Tree tree = Tree.decompose(node);

				if (tree != null)
					return isCyclic(tree.getLeft()) || isCyclic(tree.getRight());
				else
					return false;
			} finally {
				nodes.remove(idHashNode);
			}
		else
			return true;
	}

}
