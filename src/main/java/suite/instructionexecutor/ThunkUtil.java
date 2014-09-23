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

public class ThunkUtil {

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public static String evaluateToString(Fun<Node, Node> yawn, Node node) {
		StringWriter writer = new StringWriter();

		try {
			evaluateToWriter(yawn, node, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return writer.toString();
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and write
	 * corresponding characters into the writer.
	 */
	public static void evaluateToWriter(Fun<Node, Node> yawn, Node node, Writer writer) throws IOException {
		Source<Node> source = evaluateToSource(yawn, node);
		Node n;

		while ((n = source.source()) != null) {
			int c = ((Int) n).getNumber();
			writer.write(c);
			if (c == 10)
				writer.flush();
		}
	}

	public static Source<Node> evaluateToSource(Fun<Node, Node> yawn, Node node) {
		return new Source<Node>() {
			private Node node_;

			public Node source() {

				// First node is not wrapped, remainings are
				node_ = node_ != null ? yawn.apply(node_) : node;
				Tree tree;

				if ((tree = Tree.decompose(node_)) != null) {
					Node result = yawn.apply(tree.getLeft());
					node_ = tree.getRight();

					// Facilitates garbage collection
					Tree.forceSetRight(tree, null);
					return result;
				} else if (node_.finalNode() == Atom.NIL)
					return null;
				else
					throw new RuntimeException("Not a list, unable to expand");
			}
		};
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public static Node evaluateFully(Fun<Node, Node> yawn, Node node) {
		node = yawn.apply(node);

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = evaluateFully(yawn, tree.getLeft());
			Node right = evaluateFully(yawn, tree.getRight());
			node = Tree.of(tree.getOperator(), left, right);
		}

		return node;
	}

}
