package suite.fp;

import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Source;

public class LazyFunInterpreter {

	private Atom HEAD = Atom.of("head");
	private Atom IF__ = Atom.of("if");
	private Atom TAIL = Atom.of("tail");

	private static class Cons extends Node {
		private Node head;
		private Source<Node> tail;

		public Cons(Node head, Source<Node> tail) {
			this.head = head;
			this.tail = tail;
		}
	}

	public Source<Node> lazy(Node node, IMap<String, Source<Node>> env) {
		Source<Node> result;
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator operator = tree.getOperator();
			Node lhs = tree.getLeft();
			Node rhs = tree.getRight();

			if (operator == TermOp.BRACES) { // (a => b) {c}
				Node fun = lazy(lhs, env).source();
				if (fun == HEAD)
					result = () -> ((Cons) lazy(rhs, env)).head;
				else if (fun == TAIL)
					result = ((Cons) lazy(rhs, env)).tail;
				else
					result = lazy(r(fun), env.replace(v(l(fun)), lazy(rhs, env)));
			} else if (operator == TermOp.CONTD_) // a := b >> c
				result = lazy(rhs, env.put(v(l(lhs)), lazy(r(lhs), env)));
			else if (operator == TermOp.OR____) // a; b
				result = suspend(() -> new Cons(lazy(lhs, env).source(), lazy(rhs, env)));
			else if (operator == TermOp.EQUAL_) // a = b
				result = suspend(() -> i(lhs, env) == i(rhs, env) ? Atom.TRUE : Atom.FALSE);
			else if (operator == TermOp.MINUS_) // a - b
				result = suspend(() -> Int.of(i(lhs, env) - i(rhs, env)));
			else if (operator == TermOp.NOTEQ_) // a != b
				result = suspend(() -> i(lhs, env) != i(rhs, env) ? Atom.TRUE : Atom.FALSE);
			else if (operator == TermOp.PLUS__) // a + b
				result = suspend(() -> Int.of(i(lhs, env) + i(rhs, env)));
			else if (operator == TermOp.TUPLE_ && lhs == IF__) {
				Node clause = b(lazy(l(rhs), env)) ? l(r(r(rhs))) : r(r(r(r(rhs))));
				result = lazy(clause, env);
			} else
				result = () -> node;
		} else if (!(node instanceof Atom) || (result = env.get(v(node))) == null)
			result = () -> node;

		return result;
	}

	private Source<Node> suspend(Source<Node> source) {
		return new Source<Node>() {
			private Source<Node> source_ = source;
			private Node node;

			public Node source() {
				if (node == null) {
					node = source_.source();
					source_ = null;
				}
				return node;
			}
		};
	}

	private boolean b(Source<Node> node) {
		return node.source() == Atom.TRUE;
	}

	private int i(Node node, IMap<String, Source<Node>> env) {
		return ((Int) lazy(node, env).source()).getNumber();
	}

	private String v(Node node) {
		return ((Atom) node).getName();
	}

	private Node l(Node node) {
		Tree tree = Tree.decompose(node);
		return tree != null ? tree.getLeft() : null;
	}

	private Node r(Node node) {
		Tree tree = Tree.decompose(node);
		return tree != null ? tree.getRight() : null;
	}

}
