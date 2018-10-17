package suite.instructionexecutor.thunk;

import static suite.util.Friends.fail;

import java.io.IOException;
import java.io.Writer;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.primitive.IoSink;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;

public class ThunkUtil {

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public static String yawnString(Iterate<Node> yawn, Node node) {
		var st = yawnList(yawn, node, false);
		var sb = new StringBuilder();
		Node n;

		while ((n = st.next()) != null)
			sb.append((char) Int.num(n));

		return sb.toString();
	}

	public static void yawnWriter(Iterate<Node> yawn, Node node, Writer writer) throws IOException {
		ThunkUtil.yawnSink(yawn, node, n -> {
			var c = Int.num(n);
			writer.write(c);
			if (c == 10)
				writer.flush();
		});
	}

	/**
	 * Evaluates the whole (lazy) term to a list and feed the elements into a sink.
	 */
	public static void yawnSink(Iterate<Node> yawn, Node node, IoSink<Node> sink) throws IOException {
		var st = yawnList(yawn, node, true);
		Node n;
		while ((n = st.next()) != null)
			sink.f(n);
	}

	public static Outlet<Node> yawnList(Iterate<Node> yawn, Node node, boolean isFacilitateGc) {
		return Outlet.of(new Source<>() {
			private Node node_ = node;
			private boolean first = true;

			public Node g() {

				// first node is not a thunk, remainings are
				if (!first)
					node_ = yawn.apply(node_);
				else
					first = false;

				var tree = Tree.decompose(node_);

				if (tree != null) {
					var result = yawn.apply(tree.getLeft());
					node_ = tree.getRight();

					if (isFacilitateGc)
						Tree.forceSetRight(tree, null);
					return result;
				} else if (node_ == Atom.NIL)
					return null;
				else
					return fail("not a list, unable to expand");
			}
		});
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public static Node deepYawn(Iterate<Node> yawn, Node node) {
		node = yawn.apply(node);

		if (node instanceof Tree) {
			var tree = (Tree) node;
			var left = deepYawn(yawn, tree.getLeft());
			var right = deepYawn(yawn, tree.getRight());
			node = Tree.of(tree.getOperator(), left, right);
		}

		return node;
	}

}
