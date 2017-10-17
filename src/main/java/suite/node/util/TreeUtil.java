package suite.node.util;

import java.util.ArrayList;
import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.primitive.IntInt_Int;
import suite.util.FunUtil.Sink;
import suite.util.Object_;

public class TreeUtil {

	private static Atom AND = Atom.of("and");
	private static Atom OR_ = Atom.of("or");
	private static Atom SHL = Atom.of("shl");
	private static Atom SHR = Atom.of("shr");

	public static List<Node> breakdown(Operator operator, Node node) {
		List<Node> list = new ArrayList<>();
		Sink<Node> sink = Object_.fix(m -> node_ -> {
			Tree tree;
			if ((tree = Tree.decompose(node_, operator)) != null) {
				Sink<Node> sink_ = m.get();
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

	public static IntInt_Int evaluateOp(Node op) {
		if (op == AND)
			return (a, b) -> a & b;
		else if (op == OR_)
			return (a, b) -> a | b;
		else if (op == SHL)
			return (a, b) -> a << b;
		else if (op == SHR)
			return (a, b) -> a >> b;
		else
			throw new RuntimeException("cannot evaluate operator: " + op);
	}

	public static IntInt_Int evaluateOp(TermOp op) {
		switch (op) {
		case BIGAND:
			return (a, b) -> a & b;
		case BIGOR_:
			return (a, b) -> a | b;
		case PLUS__:
			return (a, b) -> a + b;
		case MINUS_:
			return (a, b) -> a - b;
		case MULT__:
			return (a, b) -> a * b;
		case DIVIDE:
			return (a, b) -> a / b;
		case MODULO:
			return (a, b) -> a % b;
		case POWER_:
			return TreeUtil::intPow;
		default:
			throw new RuntimeException("cannot evaluate operator: " + op);
		}
	}

	public static boolean isList(Node node, Operator operator) {
		Tree tree;
		return node == Atom.NIL || (tree = Tree.decompose(node, operator)) != null && isList(tree.getRight(), operator);
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

	private static int intPow(int a, int b) {
		if (b < 0)
			throw new RuntimeException();
		else if (b == 0)
			return 1;
		else {
			int p = intPow(a, b / 2);
			int pp = p * p;
			return (b % 2 == 1 ? pp * a : pp);
		}
	}

}
