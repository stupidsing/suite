package suite.node.util;

import suite.lp.Trail;
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
			public Boolean apply(Node node) {
				boolean result;
				if (!eq(node, from)) {
					NodeRead nr = NodeRead.of(node);
					result = Read.from(nr.children).fold(false, (r, p) -> r || apply(p.t1));
				} else
					result = true;
				return result;
			}
		}.apply(node);
	}

	public Node replace(Node from, Node to, Node node0) {
		return rewrite(n -> !eq(n, from) ? n : to, node0);
	}

	public Node rewrite(Node from, Node to, Node node0) {
		return rewrite(() -> {
			Cloner cloner = new Cloner();
			return new Node[] { cloner.clone(from), cloner.clone(to) };
		} , node0);
	}

	public Node rewrite(Source<Node[]> source, Node node) {
		Trail trail = new Trail();

		return rewrite(new Fun<Node, Node>() {
			public Node apply(Node n) {
				return rewrite0(n);
			}

			private Node rewrite0(Node node0) {
				Node node1;
				if (!(node0 instanceof Reference))
					node1 = bind(node0);
				else
					node1 = node0;
				return node1;
			}

			private Node bind(Node node0) {
				Node ft[] = source.source();
				int pit = trail.getPointInTime();

				if (Binder.bind(node0, ft[0], trail))
					return ft[1];
				else {
					trail.undoBinds(pit);
					return node0;
				}
			}
		}, node);
	}

	public Node rewrite(Fun<Node, Node> fun, Node node0) {
		return fun.apply(Rewriter.transform(node0, n -> rewrite(fun, n)));
	}

	private boolean eq(Node n0, Node n1) {
		return Comparer.comparer.compare(n0, n1) == 0;
	}

}
