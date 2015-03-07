package suite.node.io;

import java.util.Arrays;

import suite.immutable.IList;
import suite.node.Atom;
import suite.node.Node;
import suite.node.io.Rewriter.NodeRead;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Util;

/**
 * Lists node contents line-by-line for human-readable purpose.
 *
 * @author ywsing
 */
public class Lister {

	public String list(Node node) {
		return leaves(node).map(this::path).collect(As.joined("\n"));
	}

	private String path(IList<Node> path) {
		return Read.from(path).map(Node::toString).reverse().collect(As.joined("."));
	}

	public Streamlet<IList<Node>> leaves(Node node) {
		return leaves(node, IList.end());
	}

	private Streamlet<IList<Node>> leaves(Node node, IList<Node> prefix) {
		NodeRead nr = new NodeRead(node);
		if (!Util.stringEquals(nr.type, "term")) {
			Streamlet<IList<Node>> st = Read.from(nr.children) //
					.concatMap(p -> leaves(p.t1, IList.cons(p.t0, prefix)));
			if (nr.op != null)
				st = st.cons(IList.cons(Atom.of(nr.op.toString()), prefix));
			return st;
		} else
			return Read.from(Arrays.asList(IList.cons(nr.terminal, prefix)));
	}

}
