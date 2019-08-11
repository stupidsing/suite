package suite.node.io;

import java.util.List;

import primal.streamlet.Streamlet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.Rewrite_.NodeRead;
import suite.node.io.Rewrite_.ReadType;
import suite.persistent.PerList;
import suite.streamlet.As;
import suite.streamlet.Read;

/**
 * Lists node contents line-by-line for human-readable purpose.
 *
 * @author ywsing
 */
public class Lister {

	public String list(Node node) {
		return leaves(node).map(this::path).collect(As.conc("\n"));
	}

	private String path(PerList<Node> path) {
		return path.streamlet().map(Node::toString).reverse().collect(As.conc("."));
	}

	public Streamlet<PerList<Node>> leaves(Node node) {
		return leaves(node, PerList.end());
	}

	private Streamlet<PerList<Node>> leaves(Node node, PerList<Node> prefix) {
		var nr = NodeRead.of(node);
		Streamlet<PerList<Node>> st;

		if (nr.type == ReadType.TUPLE)
			st = Read //
					.from(nr.children) //
					.index() //
					.map((i, p) -> leaves(p.v, PerList.cons(Int.of(i), prefix))) //
					.collect(As::concat);
		else if (nr.type != ReadType.TERM)
			st = nr.children.concatMap((k, v) -> leaves(v, PerList.cons(k, prefix)));
		else
			st = Read.from(List.of(PerList.cons(nr.terminal, prefix)));

		if (nr.op != null)
			st = st.cons(PerList.cons(Atom.of(nr.op.toString()), prefix));

		return st;
	}

}
