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
	private Atom TAIL = Atom.of("tail");

	private static class Pair extends Node {
		private Source<Node> first;
		private Source<Node> second;

		public Pair(Source<Node> left, Source<Node> right) {
			this.first = left;
			this.second = right;
		}
	}

	public Source<Node> lazy(Node node) {
		IMap<String, Source<Node>> env = new IMap<>();
		env.put(HEAD.getName(), () -> HEAD);
		env.put(TAIL.getName(), () -> TAIL);
		return lazy(node, env);
	}

	private Source<Node> lazy(Node node, IMap<String, Source<Node>> env) {
		Source<Node> result;
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator operator = tree.getOperator();
			Node lhs = tree.getLeft();
			Node rhs = tree.getRight();

			if (operator == TermOp.AND___) // a, b
				result = memoize(() -> new Pair(lazy(lhs, env), lazy(rhs, env)));
			else if (operator == TermOp.BRACES) { // (a => b) {c}
				Node fun = lazy(lhs, env).source();
				if (fun == HEAD)
					result = ((Pair) lazy(rhs, env)).first;
				else if (fun == TAIL)
					result = ((Pair) lazy(rhs, env)).second;
				else
					result = lazy(r(fun), env.replace(v(l(fun)), lazy(rhs, env)));
			} else if (operator == TermOp.CONTD_) // a := b >> c
				result = lazy(rhs, env.put(v(l(lhs)), lazy(r(lhs), env)));
			else if (operator == TermOp.DIVIDE) // a / b
				result = memoize(() -> Int.of(i(lhs, env) / i(rhs, env)));
			else if (operator == TermOp.FUN___) // a => b
				result = () -> node;
			else if (operator == TermOp.EQUAL_) // a = b
				result = memoize(() -> i(lhs, env) == i(rhs, env) ? Atom.TRUE : Atom.FALSE);
			else if (operator == TermOp.MINUS_) // a - b
				result = memoize(() -> Int.of(i(lhs, env) - i(rhs, env)));
			else if (operator == TermOp.MULT__) // a * b
				result = memoize(() -> Int.of(i(lhs, env) * i(rhs, env)));
			else if (operator == TermOp.NOTEQ_) // a != b
				result = memoize(() -> i(lhs, env) != i(rhs, env) ? Atom.TRUE : Atom.FALSE);
			else if (operator == TermOp.PLUS__) // a + b
				result = memoize(() -> Int.of(i(lhs, env) + i(rhs, env)));
			else if (operator == TermOp.TUPLE_) { // if a then b else c
				Node clause = b(lazy(l(rhs), env)) ? l(r(r(rhs))) : r(r(r(r(rhs))));
				result = lazy(clause, env);
			} else
				result = () -> node;
		} else if (node instanceof Atom)
			result = env.get(v(node));
		else
			result = () -> node;

		return result;
	}

	private Source<Node> memoize(Source<Node> source) {
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
