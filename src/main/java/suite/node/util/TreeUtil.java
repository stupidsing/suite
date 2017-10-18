package suite.node.util;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.primitive.IntInt_Int;
import suite.util.FunUtil.Sink;
import suite.util.Object_;

public class TreeUtil {

	public interface IntInt_Bool {
		public boolean apply(int a, int b);
	}

	public static Atom AND = Atom.of("and");
	public static Atom OR_ = Atom.of("or");
	public static Atom SHL = Atom.of("shl");
	public static Atom SHR = Atom.of("shr");

	public static Map<Operator, IntInt_Bool> boolOperations = Map.ofEntries( //
			entry(TermOp.EQUAL_, (a, b) -> a == b), //
			entry(TermOp.NOTEQ_, (a, b) -> a != b), //
			entry(TermOp.LE____, (a, b) -> a <= b), //
			entry(TermOp.LT____, (a, b) -> a < b));

	public static Map<Operator, IntInt_Int> intOperations = Map.ofEntries( //
			entry(TermOp.BIGAND, (a, b) -> a & b), //
			entry(TermOp.BIGOR_, (a, b) -> a | b), //
			entry(TermOp.PLUS__, (a, b) -> a + b), //
			entry(TermOp.MINUS_, (a, b) -> a - b), //
			entry(TermOp.MULT__, (a, b) -> a * b), //
			entry(TermOp.DIVIDE, (a, b) -> a / b), //
			entry(TermOp.MODULO, (a, b) -> a % b), //
			entry(TermOp.POWER_, TreeUtil::intPow));

	private static Map<Node, IntInt_Int> tupleOperations = Map.ofEntries( //
			entry(AND, (a, b) -> a & b), //
			entry(OR_, (a, b) -> a | b), //
			entry(SHL, (a, b) -> a << b), //
			entry(SHR, (a, b) -> a >> b));

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

	public static int evaluate(Node node) {
		Tree tree = Tree.decompose(node);
		int result;

		if (tree != null) {
			Operator op = tree.getOperator();
			IntInt_Int fun;
			int lhs, rhs;

			if (op == TermOp.TUPLE_) {
				Tree rightTree = Tree.decompose(tree.getRight());
				lhs = evaluate(tree.getLeft());
				rhs = evaluate(rightTree.getRight());
				fun = evaluateOp(rightTree.getLeft());
			} else {
				lhs = evaluate(tree.getLeft());
				rhs = evaluate(tree.getRight());
				fun = evaluateOp(op);
			}

			result = fun.apply(lhs, rhs);
		} else if (node instanceof Int)
			result = ((Int) node).number;
		else
			throw new RuntimeException("cannot evaluate expression: " + node);

		return result;
	}

	public static IntInt_Int evaluateOp(Node op) {
		IntInt_Int fun = tupleOperations.get(op);
		if (fun != null)
			return fun;
		else
			throw new RuntimeException("cannot evaluate operator: " + op);
	}

	public static IntInt_Int evaluateOp(Operator op) {
		IntInt_Int fun = intOperations.get(op);
		if (fun != null)
			return fun;
		else
			throw new RuntimeException("cannot evaluate operator: " + op);
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
