package suite.instructionexecutor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class ExpandUtil {

	public static void expandList(Fun<Node, Node> unwrapper, Node node, Sink<Node> sink) {
		Tree tree;

		while ((tree = Tree.decompose(node)) != null) {
			sink.sink(unwrapper.apply(tree.getLeft()));
			node = unwrapper.apply(tree.getRight());
			Tree.forceSetRight(tree, null); // Facilitates garbage collection
		}

		if (node != Atom.NIL)
			throw new RuntimeException("Not a list, unable to expand");
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
	public static void expandToWriter(final Fun<Node, Node> unwrapper, Node node, final Writer writer) throws IOException {
		expandList(unwrapper, node, new Sink<Node>() {
			public void sink(Node node) {
				try {
					int c = ((Int) node).getNumber();
					writer.write(c);
					if (c == 10)
						writer.flush();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
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
