package org.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.parser.Operator;
import org.suite.doer.Comparer;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.node.Tree;

public class Interpreter {

	private Map<Atom, Node> functions = new TreeMap<Atom, Node>();

	private static final Atom ELSE = Atom.create("else");
	private static final Atom FALSE = Atom.create("false");
	private static final Atom IF = Atom.create("if");
	private static final Atom LEFT = Atom.create("left");
	private static final Atom NOT = Atom.create("not");
	private static final Atom OPER = Atom.create("oper");
	private static final Atom PLAIN = Atom.create("p");
	private static final Atom RIGHT = Atom.create("right");
	private static final Atom SWITCH = Atom.create("switch");
	private static final Atom THEN = Atom.create("then");
	private static final Atom TREE = Atom.create("tree");
	private static final Atom TRUE = Atom.create("true");

	/**
	 * Interprets a function call, by expanding the node and simplifying it
	 * repeatedly.
	 */
	public Node evaluate(Node node) {
		Node lastExpanded = Atom.nil;

		while (Comparer.comparer.compare(lastExpanded, node) != 0) {
			node = expand(lastExpanded = node);

			Node lastSimplified = Atom.nil;
			while (Comparer.comparer.compare(lastSimplified, node) != 0)
				node = simplify(lastSimplified = node);
		}

		return node;
	}

	public void addFunctions(Node node) {
		for (Node f : flatten(node, TermOp.NEXT__)) {
			Tree tree = Tree.decompose(f, TermOp.EQUAL_);
			if (tree != null)
				addFunction((Atom) tree.getLeft(), tree.getRight());
		}
	}

	public void addFunction(Atom head, Node body) {
		functions.put(head, body);
	}

	/**
	 * Expands a node using the function mappings as much as possible, until we
	 * recur into the same construct.
	 */
	private Node expand(Node node) {
		return expand(node, new TreeSet<Atom>());
	}

	private Node expand(Node node, Set<Atom> expanded) {
		node = node.finalNode();

		if (node instanceof Atom) {
			Atom atom = (Atom) node;
			if (!expanded.contains(atom)) {
				expanded.add(atom);
				Node definition = functions.get(atom);
				if (definition != null)
					return expand(definition, expanded);
			}
		} else if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = expand(l, expanded), gr = expand(r, expanded);
			if (gl != l || gr != r)
				return new Tree(t.getOperator(), gl, gr);
		}

		return node;
	}

	private Node simplify(Node node) {

		// Late and lazy evaluation; evaluates a reference once and for all
		if (node instanceof EvaluatableReference) {
			EvaluatableReference reference = (EvaluatableReference) node;
			if (!reference.evaluated)
				reference.bound(evaluate(reference.finalNode()));
		}

		Tree tree = Tree.decompose(node);
		if (tree != null)
			node = simplifyTree(tree);
		return node;
	}

	private Node simplifyTree(Tree tree) {
		TermOp operator = (TermOp) tree.getOperator();
		Node l = tree.getLeft(), r = tree.getRight();

		if (operator == TermOp.SEP___) {
			List<Node> list = flatten(tree, TermOp.SEP___);
			Node name = list.get(0);
			if (name == IF && list.get(2) == THEN && list.get(4) == ELSE)
				return ifThenElse(list.get(1), list.get(3), list.get(5));
			else if (name == SWITCH)
				return doSwitch(list);
			else if (name == NOT)
				return evaluate(list.get(1)) == TRUE ? FALSE : TRUE;
			else if (name == PLAIN)
				return list.get(1);
		} else if (operator == TermOp.DIVIDE) { // Substitution
			if (l == TREE || l == LEFT || l == RIGHT || l == OPER)
				return doTreeFunction(l, r);
			else {
				Tree lambda = Tree.decompose(l, TermOp.INDUCE);
				Node lazy = new EvaluatableReference(r);

				if (lambda != null)
					return replace(lambda.getRight(), lambda.getLeft(), lazy);
			}
		} else if (operator == TermOp.AND___)
			return evaluate(l) == TRUE && evaluate(r) == TRUE ? TRUE : FALSE;
		else if (operator == TermOp.OR____)
			return evaluate(l) == TRUE || evaluate(r) == TRUE ? TRUE : FALSE;
		else if (operator == TermOp.EQUAL_)
			return eq(evaluate(l), evaluate(r));
		else {
			int n1 = getNumber(evaluate(l)), n2 = getNumber(evaluate(r));

			switch (operator) {
			case LT____:
				return n1 < n2 ? TRUE : FALSE;
			case LE____:
				return n1 <= n2 ? TRUE : FALSE;
			case GT____:
				return n1 > n2 ? TRUE : FALSE;
			case GE____:
				return n1 >= n2 ? TRUE : FALSE;
			case PLUS__:
				return Int.create(n1 + n2);
			case MINUS_:
				return Int.create(n1 - n2);
			case MULT__:
				return Int.create(n1 * n2);
			case DIVIDE:
				return Int.create(n1 / n2);
			}
		}

		// throw new RuntimeException("Cannot simplify " +
		// Formatter.dump(tree));

		return tree;
	}

	private Node doTreeFunction(Node name, Node parameter) {
		Tree t = Tree.decompose(parameter);

		if (name == TREE)
			return t != null ? TRUE : FALSE;
		else if (name == OPER)
			return new Str(t.getOperator().getName());
		else
			return name == LEFT ? t.getLeft() : t.getRight();
	}

	private Atom eq(Node left, Node right) {
		return Comparer.comparer.compare(left, right) == 0 ? TRUE : FALSE;
	}

	private Node ifThenElse(Node if_, Node then_, Node else_) {
		return evaluate(if_) == TRUE ? then_ : else_;
	}

	private Node doSwitch(List<Node> list) {
		int last = list.size() - 1;
		for (int i = 1; i < last; i++) {
			Tree t = Tree.decompose(list.get(i), TermOp.INDUCE);
			if (t != null) {
				if (evaluate(t.getLeft()) == TRUE)
					return evaluate(t.getRight());
			} else
				throw new RuntimeException("Bad switch definition");
		}
		return evaluate(list.get(last));
	}

	private static Node replace(Node node, Node from, Node to) {
		node = node.finalNode();

		if (node == from)
			return to;
		else if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = replace(l, from, to), gr = replace(r, from, to);
			if (gl != l || gr != r)
				return new Tree(t.getOperator(), gl, gr);
		}

		return node;
	}

	private static List<Node> flatten(Node node, Operator operator) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		Tree tree;

		while ((tree = Tree.decompose(node, operator)) != null) {
			nodes.add(tree.getLeft());
			node = tree.getRight();
		}

		nodes.add(node);
		return nodes;
	}

	private int getNumber(Node node) {
		return ((Int) node).getNumber();
	}

}
