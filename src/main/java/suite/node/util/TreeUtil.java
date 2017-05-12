package suite.node.util;

import java.util.ArrayList;
import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Sink;

public class TreeUtil {

	public static List<Node> breakdown(Operator operator, Node node) {
		List<Node> list = new ArrayList<>();
		Mutable<Sink<Node>> mutableSink = Mutable.nil();
		Sink<Node> sink;
		mutableSink.set(sink = node_ -> {
			Tree tree;
			if ((tree = Tree.decompose(node_, operator)) != null) {
				Sink<Node> sink_ = mutableSink.get();
				sink_.sink(tree.getLeft());
				sink_.sink(tree.getRight());
			} else
				list.add(node_);
		});
		sink.sink(node);
		return list;
	}

	public static Node[] elements(Node node0, int n) {
		Node[] params = new Node[n];
		Node node = node0;
		Tree tree;
		for (int i = 0; i < n - 1; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				params[i] = tree.getLeft();
				node = tree.getRight();
			} else
				throw new RuntimeException("not enough parameters in " + node0);
		params[n - 1] = node;
		return params;
	}

	public static int nElements(Node node) {
		int n = 1;
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}
		return n;
	}

	public static boolean isList(Node node, Operator operator) {
		Tree tree;
		return node == Atom.NIL || (tree = Tree.decompose(node, operator)) != null && isList(tree.getRight(), operator);
	}

}
