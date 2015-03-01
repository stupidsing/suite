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
			} else if ((tree = Tree.decompose(node)) != null) {
				Operator operator = tree.getOperator();
				if (Arrays.asList(TermOp.AND___, TermOp.OR____).contains(operator)) {
					Streamlet<Node> st = Read.from(Tree.iter(node, operator));
					type = operator.toString();
					children = st.index((i, n) -> Pair.of(i.toString(), n)).toList();
				} else {
					Pair<String, Node> p0 = Pair.of("l", tree.getLeft());
					Pair<String, Node> p1 = Pair.of("r", tree.getRight());
					type = operator.toString();
					children = Arrays.asList(p0, p1);
				}
			} else {
				type = "";
				children = Collections.emptyList();
			}
		}
	}

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
		NodeReader nr = new NodeReader(node);
		if (!nr.children.isEmpty())
			return Read.from(nr.children).concatMap(p -> leaves(p.t1, IList.cons(new Str(p.t0), prefix)));
		else
			return Read.from(Arrays.asList(IList.cons(node, prefix)));
	}

}
