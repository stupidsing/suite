package suite.node.util;

import primal.MoreVerbs.Read;
import primal.parser.Operator;
import primal.primitive.IntInt_Int;
import primal.streamlet.Streamlet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.IntInt_Bool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static primal.statics.Fail.fail;

public class TreeUtil {

	public static Atom AND = Atom.of("and");
	public static Atom OR_ = Atom.of("or");
	public static Atom SHL = Atom.of("shl");
	public static Atom SHR = Atom.of("shr");
	public static Atom XOR = Atom.of("xor");

	public static Map<Operator, IntInt_Bool> boolOperations = Map.ofEntries(
			entry(TermOp.EQUAL_, (a, b) -> a == b),
			entry(TermOp.NOTEQ_, (a, b) -> a != b),
			entry(TermOp.LE____, (a, b) -> a <= b),
			entry(TermOp.LT____, (a, b) -> a < b));

	public static Map<Operator, IntInt_Int> intOperations = Map.ofEntries(
			entry(TermOp.BIGAND, (a, b) -> a & b),
			entry(TermOp.BIGOR_, (a, b) -> a | b),
			entry(TermOp.PLUS__, (a, b) -> a + b),
			entry(TermOp.MINUS_, (a, b) -> a - b),
			entry(TermOp.MULT__, (a, b) -> a * b),
			entry(TermOp.DIVIDE, (a, b) -> a / b),
			entry(TermOp.MODULO, (a, b) -> a % b),
			entry(TermOp.POWER_, TreeUtil::intPow));

	public static Map<Node, IntInt_Int> tupleOperations = Map.ofEntries(
			entry(AND, (a, b) -> a & b),
			entry(OR_, (a, b) -> a | b),
			entry(SHL, (a, b) -> a << b),
			entry(SHR, (a, b) -> a >> b),
			entry(XOR, (a, b) -> a ^ b));

	public static Streamlet<Node> breakdown(Operator operator, Node node) {
		var list = new ArrayList<Node>();
		new Object() {
			private void breakdown(Node node_) {
				var tree = Tree.decompose(node_, operator);
				if (tree != null) {
					breakdown(tree.getLeft());
					breakdown(tree.getRight());
				} else
					list.add(node_);
			}
		}.breakdown(node);
		return Read.from(list);
	}

	public static Node buildUp(Operator operator, List<Node> nodes) {
		Node result = Atom.NIL;
		var i = nodes.size();
		while (0 <= --i)
			result = Tree.of(operator, nodes.get(i), result);
		return result;
	}

	public static Node[] elements(Node node0, int n) {
		var params = new Node[n];
		var node = node0;
		Tree tree;
		for (var i = 0; i < n - 1; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				params[i] = tree.getLeft();
				node = tree.getRight();
			} else
				fail("not enough parameters in " + node0);
		params[n - 1] = node;
		return params;
	}

	public static int evaluate(Node node) {
		var tree = Tree.decompose(node);

		if (tree != null) {
			var op = tree.getOperator();
			IntInt_Int fun;
			int lhs, rhs;

			if (op == TermOp.TUPLE_) {
				var rightTree = Tree.decompose(tree.getRight());
				lhs = evaluate(tree.getLeft());
				rhs = evaluate(rightTree.getRight());
				fun = evaluateOp(rightTree.getLeft());
			} else {
				lhs = evaluate(tree.getLeft());
				rhs = evaluate(tree.getRight());
				fun = evaluateOp(op);
			}

			return fun.apply(lhs, rhs);
		} else if (node instanceof Int)
			return Int.num(node);
		else
			return fail("cannot evaluate expression: " + node);
	}

	public static IntInt_Int evaluateOp(Node op) {
		var fun = tupleOperations.get(op);
		return fun != null ? fun : fail("cannot evaluate operator: " + op);
	}

	public static IntInt_Int evaluateOp(Operator op) {
		var fun = intOperations.get(op);
		return fun != null ? fun : fail("cannot evaluate operator: " + op);
	}

	public static boolean isList(Node node, Operator operator) {
		Tree tree;
		return node == Atom.NIL || (tree = Tree.decompose(node, operator)) != null && isList(tree.getRight(), operator);
	}

	public static int nElements(Node node) {
		var n = 1;
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}
		return n;
	}

	private static int intPow(int a, int b) {
		if (b < 0)
			return fail();
		else if (b == 0)
			return 1;
		else {
			int p = intPow(a, b / 2);
			var pp = p * p;
			return (b % 2 == 1 ? pp * a : pp);
		}
	}

}
