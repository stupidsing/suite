package suite.node.io;

import java.util.Arrays;

import suite.immutable.IList;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.ReadType;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

/**
 * Lists node contents line-by-line for human-readable purpose.
 *
 * @author ywsing
 */
public class Lister {

	public String list(Node node) {
		return leaves(node).map(this::path).collect(As.conc("\n"));
	}

	private String path(IList<Node> path) {
		return Read.from(path).map(Node::toString).reverse().collect(As.conc("."));
	}

	public Streamlet<IList<Node>> leaves(Node node) {
		return leaves(node, IList.end());
	}

	private Streamlet<IList<Node>> leaves(Node node, IList<Node> prefix) {
		NodeRead nr = NodeRead.of(node);
		Streamlet<IList<Node>> st;

		if (nr.type == ReadType.TUPLE)
			st = Read.from(nr.children) //
					.index((i, p) -> leaves(p.t1, IList.cons(Int.of(i), prefix))) //
					.collect(As::concat);
		else if (nr.type != ReadType.TERM)
			st = Read.from(nr.children).concatMap(p -> leaves(p.t1, IList.cons(p.t0, prefix)));
		else
			st = Read.from(Arrays.asList(IList.cons(nr.terminal, prefix)));

		if (nr.op != null)
			st = st.cons(IList.cons(Atom.of(nr.op.toString()), prefix));

		return st;
	}

}
