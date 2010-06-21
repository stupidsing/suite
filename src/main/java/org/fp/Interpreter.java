package org.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.suite.doer.Comparer;
import org.suite.doer.Parser.Operator;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;

public class Interpreter {

	private Map<Atom, Node> functions = new TreeMap<Atom, Node>();

	private static final Node WILDCARD = Atom.create("_");

	private static final Atom ELSE = Atom.create("else");
	private static final Atom FALSE = Atom.create("false");
	private static final Atom LET = Atom.create("let");
	private static final Atom IF = Atom.create("if");
	private static final Atom IN = Atom.create("in");
	private static final Atom THEN = Atom.create("then");
	private static final Atom TRUE = Atom.create("true");

	/**
	 * Interpretes a function call, by expanding the node and simplifying it
	 * repeatedly.
	 */
	public Node evaluate(Node node) {
		Node lastExpanded = null;

		while (Comparer.comparer.compare(lastExpanded, node) != 0) {
			node = expand(lastExpanded = node);

			Node lastSimplified = null;
			while (Comparer.comparer.compare(lastSimplified, node) != 0)
				node = simplify(lastSimplified = node);
		}

		return node;
	}

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
					return expand(generalize(definition), expanded);
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
		Tree tree = Tree.decompose(node);

		if (tree.getOperator() == Operator.EQUAL_)
			return eq(tree.getLeft(), tree.getRight());
		else if (tree.getOperator() == Operator.SEP___) {
			List<Node> list = flatten(node);
			Node name = list.get(0);

			if (name == IF && list.get(2) == THEN && list.get(4) == ELSE)
				return ifThenElse(list.get(1), list.get(3), list.get(5));
			else if (name == LET && list.get(2) == IN)
				return letIn(list.get(1), list.get(3));
		} else if (tree.getOperator() == Operator.DIVIDE) { // Substitution
			Node definition = tree.getLeft();
			Node parameter = tree.getRight();
			Tree lambda = Tree.decompose(definition, Operator.INDUCE);

			if (lambda != null && bind(lambda.getLeft(), parameter))
				return lambda.getRight();
		}

		if (tree != null) {

			// Simplifies left and right individually
			Node l = tree.getLeft(), r = tree.getRight();
			Node gl = simplify(l), gr = simplify(r);
			if (gl != l || gr != r)
				return new Tree(tree.getOperator(), gl, gr);
		}

		return node;
	}

	private Atom eq(Node left, Node right) {
		return Comparer.comparer.compare(left, right) == 0 ? TRUE : FALSE;
	}

	private Node ifThenElse(Node if_, Node then_, Node else_) {
		return evaluate(if_) == TRUE ? then_ : else_;
	}

	private Node letIn(Node let_, Node in_) {
		for (Node assignment : flatten(let_, Operator.AND___)) {
			Tree t = Tree.decompose(assignment, Operator.EQUAL_);
			assert bind(t.getLeft(), t.getRight());
		}
		return in_;
	}

	private static Node generalize(Node node) {
		Map<Node, Reference> variables = new HashMap<Node, Reference>();

		node = node.finalNode();

		if (node == WILDCARD)
			return new Reference();
		else if (node instanceof Atom) {
			Reference reference;
			if (!variables.containsKey(node)) {
				reference = new Reference();
				variables.put(node, reference);
			} else
				reference = variables.get(node);
			return reference;
		} else if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = generalize(l), gr = generalize(r);
			if (gl != l || gr != r)
				return new Tree(t.getOperator(), gl, gr);
		}

		return node;
	}

	private static boolean bind(Node n1, Node n2) {
		n1 = n1.finalNode();
		n2 = n2.finalNode();

		if (n1 instanceof Reference) {
			((Reference) n1).bound(n2);
			return true;
		} else if (n2 instanceof Reference) {
			((Reference) n2).bound(n1);
			return true;
		}

		if (n1 == n2)
			return true;

		Class<? extends Node> clazz1 = n1.getClass();
		Class<? extends Node> clazz2 = n2.getClass();

		if (clazz1 != clazz2)
			return false;
		else if (clazz1 == Tree.class) {
			Tree t1 = (Tree) n1;
			Tree t2 = (Tree) n2;
			return t1.getOperator() == t2.getOperator()
					&& bind(t1.getLeft(), t2.getLeft())
					&& bind(t1.getRight(), t2.getRight());
		} else
			return Comparer.comparer.compare(n1, n2) == 0;
	}

	private static List<Node> flatten(Node node) {
		return flatten(node, Operator.SEP___);
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

}
