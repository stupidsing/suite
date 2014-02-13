package suite.instructionexecutor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class ExpandUtil {

	public static Source<Node> expandList(final Fun<Node, Node> unwrapper, final Node node) {
		return new Source<Node>() {
			private Node node_ = node;

			public Node source() {
				Tree tree;
				if ((tree = Tree.decompose(node_)) != null) {
					node_ = unwrapper.apply(tree.getRight());

					// Facilitates garbage collection
					Tree.forceSetRight(tree, null);
					return unwrapper.apply(tree.getLeft());
				} else if (node_ == Atom.NIL)
					return null;
				else
					throw new RuntimeException("Not a list, unable to expand");
			}
		};
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public static String expandString(Fun<Node, Node> unwrapper, Node node) {
		StringWriter writer = new StringWriter();

		try {
			expandToWriter(unwrapper, node, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return writer.toString();
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and write
	 * corresponding characters into the writer.
	 */
	public static void expandToWriter(Fun<Node, Node> unwrapper, Node node, Writer writer) throws IOException {
		Source<Node> source = expandList(unwrapper, node);
		Node n;

		while ((n = source.source()) != null) {
			int c = ((Int) n).getNumber();
			writer.write(c);
			if (c == 10)
				writer.flush();
		}
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public static Node expandFully(Fun<Node, Node> unwrapper, Node node) {
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = expandFully(unwrapper, unwrapper.apply(tree.getLeft()));
			Node right = expandFully(unwrapper, unwrapper.apply(tree.getRight()));
			node = Tree.create(tree.getOperator(), left, right);
		}

		return node;
	}

}
