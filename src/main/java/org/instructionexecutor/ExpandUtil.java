package org.instructionexecutor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.util.FunUtil.Fun;

public class ExpandUtil {

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public static String expandString(Node node, Fun<Node, Node> unwrapper) {
		StringWriter writer = new StringWriter();

		try {
			expand(node, unwrapper, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return writer.toString();
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and write
	 * corresponding characters into the writer.
	 */
	public static void expand(Node node, Fun<Node, Node> unwrapper, Writer writer) throws IOException {
		while (true) {
			node = unwrapper.apply(node);

			if (node instanceof Tree) {
				Tree tree = (Tree) node;
				writer.write(((Int) unwrapper.apply(tree.getLeft())).getNumber());
				node = tree.getRight();
			} else if (node == Atom.NIL)
				break;
		}
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public static Node expand(Node node, Fun<Node, Node> unwrapper) {
		node = unwrapper.apply(node);

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = expand(tree.getLeft(), unwrapper);
			Node right = expand(tree.getRight(), unwrapper);
			node = Tree.create(tree.getOperator(), left, right);
		}

		return node;
	}

}
