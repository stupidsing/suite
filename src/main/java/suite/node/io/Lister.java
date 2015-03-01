package suite.node.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.immutable.IList;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.To;

/**
 * Lists node contents line-by-line for human-readable purpose.
 *
 * @author ywsing
 */
public class Lister {

	public static class NodeReader {
		public final String type;
		public final List<Pair<String, Node>> children;

		public NodeReader(Node node) {
			Tree tree;
			if (node instanceof Dict) {
				Map<Node, Reference> map = ((Dict) node).map;
				type = "dict";
				children = Read.from(map).map(p -> Pair.of(p.t0.toString(), (Node) p.t1)).toList();
			} else if (Tree.decompose(node, TermOp.AND___) != null) {
				Streamlet<Node> st = Read.from(To.source(Tree.iter(node, TermOp.AND___)));
				type = TermOp.AND___.toString();
				children = st.index((i, n) -> Pair.of(i.toString(), n)).toList();
			} else if ((tree = Tree.decompose(node)) != null) {
				Pair<String, Node> p0 = Pair.of("l", tree.getLeft());
				Pair<String, Node> p1 = Pair.of("r", tree.getRight());
				type = tree.getOperator().toString();
				children = Arrays.asList(p0, p1);
			} else {
				type = "";
				children = Collections.emptyList();
			}
		}
	}

	public String list(Node node) {
		return leaves(node) //
				.map(path -> Read.from(To.source(path)).map(Node::toString).reverse().collect(As.joined("."))) //
				.collect(As.joined("\n"));
	}

	public Streamlet<IList<Node>> leaves(Node node) {
		return leaves(node, IList.end());
	}

	private Streamlet<IList<Node>> leaves(Node node, IList<Node> prefix) {
		NodeReader nr = new NodeReader(node);
		if (!nr.children.isEmpty()) {
			return Read.from(nr.children).concatMap(p -> leaves(p.t1, IList.cons(new Str(p.t0), prefix)));
		} else
			return Read.from(Arrays.asList(IList.cons(node, prefix)));
	}

}
