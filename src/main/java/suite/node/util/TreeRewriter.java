package suite.node.util;

import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewriter;
import suite.node.io.Rewriter.NodeRead;
import suite.streamlet.Read;

public class TreeRewriter {

	private Node from, to;
	private Journal journal;

	public TreeRewriter(Node from) {
		this(from, Atom.NIL);
	}

	public TreeRewriter(Node from, Node to) {
		this.from = from;
		this.to = to;
	}

	public boolean contains(Node node) {
		node = node.finalNode();
		boolean result;
		if (!node.equals(from)) {
			NodeRead nr = new NodeRead(node);
			result = Read.from(nr.children).fold(false, (r, p) -> r || contains(p.t1));
		} else
			result = true;
		return result;
	}

	public Node replace(Node node0) {
		node0 = node0.finalNode();
		Node node1 = !node0.equals(from) ? Rewriter.transform(node0, this::replace) : to;
		return node1;
	}

	public Node rewrite(Node node0) {
		journal = new Journal();
		try {
			return rewrite0(node0);
		} finally {
			journal = null;
		}
	}

	private Node rewrite0(Node node0) {
		node0 = node0.finalNode();
		Node node1;
		if (node0 instanceof Reference || (node1 = bind(node0)) == null)
			node1 = Rewriter.transform(node0, this::rewrite0);
		return node1;
	}

	private Node bind(Node node0) {
		Cloner cloner = new Cloner();
		Node from1 = cloner.clone(from);
		Node to1 = cloner.clone(to);

		int pit = journal.getPointInTime();

		if (Binder.bind(node0, from1, journal))
			return to1;
		else {
			journal.undoBinds(pit);
			return null;
		}
	}

}
