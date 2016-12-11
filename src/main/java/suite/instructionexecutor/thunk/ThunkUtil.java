package suite.instructionexecutor.thunk;

import java.io.IOException;
import java.io.Writer;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.primitive.IoSink;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class ThunkUtil {

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public static String yawnString(Fun<Node, Node> yawn, Node node) {
		Outlet<Node> st = yawnList(yawn, node, false);
		StringBuilder sb = new StringBuilder();
		Node n;

		while ((n = st.next()) != null)
			sb.append((char) ((Int) n).number);

		return sb.toString();
	}

	public static void yawnWriter(Fun<Node, Node> yawn, Node node, Writer writer) throws IOException {
		ThunkUtil.yawnSink(yawn, node, n -> {
			int c = ((Int) n).number;
			writer.write(c);
			if (c == 10)
				writer.flush();
		});
	}

	/**
	 * Evaluates the whole (lazy) term to a list and feed the elements into a
	 * sink.
	 */
	public static void yawnSink(Fun<Node, Node> yawn, Node node, IoSink<Node> sink) throws IOException {
		Outlet<Node> st = yawnList(yawn, node, true);
		Node n;
		while ((n = st.next()) != null)
			sink.sink(n);
	}

	public static Outlet<Node> yawnList(Fun<Node, Node> yawn, Node node, boolean isFacilitateGc) {
		return new Outlet<>(new Source<Node>() {
			private Node node_ = node;
			private boolean first = true;

			public Node source() {
				Tree tree;

				// first node is not a thunk, remainings are
				if (!first)
					node_ = yawn.apply(node_);
				else
					first = false;

				if ((tree = Tree.decompose(node_)) != null) {
					Node result = yawn.apply(tree.getLeft());
					node_ = tree.getRight();

					if (isFacilitateGc)
						Tree.forceSetRight(tree, null);
					return result;
				} else if (node_ == Atom.NIL)
					return null;
				else
					throw new RuntimeException("Not a list, unable to expand");
			}
		});
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public static Node yawnFully(Fun<Node, Node> yawn, Node node) {
		node = yawn.apply(node);

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = yawnFully(yawn, tree.getLeft());
			Node right = yawnFully(yawn, tree.getRight());
			node = Tree.of(tree.getOperator(), left, right);
		}

		return node;
	}

}
