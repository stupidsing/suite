package suite.node.util;

import java.util.List;
import java.util.Objects;

import suite.adt.Pair;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.ReadWrite.NodeRead;
import suite.node.io.ReadWrite.NodeWrite;
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
			NodeRead nr = new NodeRead(node);
			result = Read.from(nr.children).fold(false, (r, p) -> r && contains(p.t1));
		} else
			result = true;
		return result;
	}

	public Node replace(Node node0) {
		node0 = node0.finalNode();
		Node node1;
		if (!node0.equals(from)) {
			NodeRead nr = new NodeRead(node0);
			List<Pair<Node, Node>> children1 = Read.from(nr.children) //
					.map(p -> Pair.of(p.t0, replace(p.t1))) //
					.toList();
			NodeWrite nw = new NodeWrite(nr.type, nr.terminal, nr.op, children1);
			node1 = nw.node;
		} else
			node1 = to;
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

		if (node0 instanceof Reference || !bind(node0)) {
			NodeRead nr = new NodeRead(node0);
			List<Pair<Node, Node>> children1 = Read.from(nr.children) //
					.map(p -> Pair.of(p.t0, rewrite0(p.t1))) //
					.toList();

			if (!Objects.equals(nr.children, children1))
				node1 = new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
			else
				node1 = node0;
		} else
			node1 = to1;

		return node1;
	}

	private boolean bind(Node node0) {
		Cloner cloner = new Cloner();
		from1 = cloner.clone(from);
		to1 = cloner.clone(to);

		int pit = journal.getPointInTime();
		boolean result = Binder.bind(node0, from1, journal);
		if (!result)
			journal.undoBinds(pit);
		return result;
	}

}
