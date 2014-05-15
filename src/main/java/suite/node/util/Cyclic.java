package suite.node.util;

import java.util.HashSet;
import java.util.Set;

import suite.node.Node;
import suite.node.Tree;
import suite.util.Util;

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
			return Util.clazz(object) == IdHashNode.class && node == ((IdHashNode) object).node;
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
