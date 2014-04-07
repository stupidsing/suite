package suite.node.util;

import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Tree;

public class Rewriter {

	private Node from, to;
	private Node from1, to1;

	public Rewriter(Node from, Node to) {
		this.from = from;
		this.to = to;
	}

	public Node rewrite(Node node0) {
		reclone();
		return rewrite(node0, new Journal());
	}

	private Node rewrite(Node node0, Journal journal) {
		Node node1;

		if (!Binder.bind(node0, from1, journal)) {
			Tree tree = Tree.decompose(node0);

			if (tree != null)
				node1 = Tree.create(tree.getOperator() //
						, rewrite(tree.getLeft(), journal) //
						, rewrite(tree.getRight(), journal));
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
