package suite.node.util;

import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewriter;
import suite.node.io.Rewriter.NodeRead;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class TreeRewriter {

	public boolean contains(Node from, Node node) {
		return new Fun<Node, Boolean>() {
			public Boolean apply(Node node0) {
				Node node = node0.finalNode();
				boolean result;
				if (!node.equals(from)) {
					NodeRead nr = NodeRead.of(node);
					result = Read.from(nr.children).fold(false, (r, p) -> r || apply(p.t1));
				} else
					result = true;
				return result;
			}
		}.apply(node);
	}

	public Node replace(Node from, Node to, Node node0) {
		return new Fun<Node, Node>() {
			public Node apply(Node node0) {
				node0 = node0.finalNode();
				return !node0.equals(from) ? Rewriter.transform(node0, this::apply) : to;
			}
		}.apply(node0);
	}

	public Node rewrite(Node from, Node to, Node node0) {
		return rewrite(() -> {
			Cloner cloner = new Cloner();
			return new Node[] { cloner.clone(from), cloner.clone(to) };
		}, node0);
	}

	public Node rewrite(Source<Node[]> source, Node node0) {
		Journal journal = new Journal();

		return new Source<Node>() {
			public Node source() {
				return rewrite0(node0);
			}

			private Node rewrite0(Node node0) {
				node0 = node0.finalNode();
				Node node1;
				if (node0 instanceof Reference || (node1 = bind(node0)) == null)
					node1 = Rewriter.transform(node0, this::rewrite0);
				return node1;
			}

			private Node bind(Node node0) {
				Node ft[] = source.source();
				int pit = journal.getPointInTime();

				if (Binder.bind(node0, ft[0], journal))
					return ft[1];
				else {
					journal.undoBinds(pit);
					return null;
				}
			}
		}.source();
	}

}
