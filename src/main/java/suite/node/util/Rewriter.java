package suite.node.util;

import java.util.List;

import suite.adt.Pair;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Lister.NodeReader;
import suite.node.io.Lister.NodeWriter;
import suite.streamlet.Read;

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
		node = node.finalNode();
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
		node0 = node0.finalNode();
		Node node1;
		if (!node0.equals(from)) {
			NodeReader nr = new NodeReader(node0);
			List<Pair<Node, Node>> children1 = Read.from(nr.children) //
					.map(p -> Pair.of(p.t0, replace(p.t1))) //
					.toList();
			NodeWriter nw = new NodeWriter(nr.type, nr.terminal, nr.op, children1);
			node1 = nw.node;
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

			if (tree != null) {
				Node left = tree.getLeft();
				Node right = tree.getRight();
				Node left1 = rewrite0(left);
				Node right1 = rewrite0(right);
				if (left != left1 || right != right1)
					node1 = Tree.of(tree.getOperator(), left1, right1);
				else
					node1 = node0;
			} else
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
