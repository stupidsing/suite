package suite.node.util;

import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;

public class Rewriter {

	private Node from, to;
	private Node from1, to1;
	private Journal journal;

	public Rewriter(Node from) {
		this(from, Atom.NIL);
	}

	public Rewriter(Node from, Node to) {
		this.from = from;
		this.to = to;
	}

	public boolean contains(Node node) {
		boolean result;

		if (!node.equals(from)) {
			Tree tree = Tree.decompose(node);

			if (tree != null)
				result = contains(tree.getLeft()) || contains(tree.getRight());
			else
				result = false;
		} else
			result = true;

		return result;
	}

	public Node replace(Node node0) {
		Node node1;

		if (!node0.equals(from)) {
			Tree tree = Tree.decompose(node0);

			if (tree != null)
				node1 = Tree.of(tree.getOperator(), replace(tree.getLeft()), replace(tree.getRight()));
			else
				node1 = node0;
		} else
			node1 = to;

		return node1;
	}

	public Node rewrite(Node node0) {
		journal = new Journal();
		try {
			reclone();
			return rewrite0(node0);
		} finally {
			journal = null;
		}
	}

	private Node rewrite0(Node node0) {
		node0 = node0.finalNode();
		Node node1;

		if (node0 instanceof Reference || !Binder.bind(node0, from1, journal)) {
			Tree tree = Tree.decompose(node0);

			if (tree != null)
				node1 = Tree.of(tree.getOperator(), rewrite0(tree.getLeft()), rewrite0(tree.getRight()));
			else
				node1 = node0;
		} else {
			node1 = to1;
			reclone();
		}

		return node1;
	}

	private void reclone() {
		Cloner cloner = new Cloner();
		from1 = cloner.clone(from);
		to1 = cloner.clone(to);
	}

}
