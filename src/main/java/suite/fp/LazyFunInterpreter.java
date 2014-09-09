package suite.fp;

import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class LazyFunInterpreter {

	private Atom ERROR = Atom.of("error");
	private Atom FST__ = Atom.of("fst");
	private Atom SND__ = Atom.of("snd");

	private static class Fun_ extends Node {
		private Fun<Source<Node>, Source<Node>> fun;

		public Fun_(Fun<Source<Node>, Source<Node>> fun) {
			this.fun = fun;
		}
	}

	private static class Pair_ extends Node {
		private Source<Node> first;
		private Source<Node> second;

		public Pair_(Source<Node> left, Source<Node> right) {
			this.first = left;
			this.second = right;
		}
	}

	public Source<Node> lazy(Node node) {
		Source<Node> equal = () -> new Fun_(a -> () -> new Fun_(b -> () -> i(a) == i(b) ? Atom.TRUE : Atom.FALSE));
		Source<Node> noteq = () -> new Fun_(a -> () -> new Fun_(b -> () -> i(a) != i(b) ? Atom.TRUE : Atom.FALSE));
		Source<Node> error = () -> {
			throw new RuntimeException("Error termination");
		};

		IMap<String, Source<Node>> env = new IMap<>();
		env = env.put(Atom.TRUE.getName(), () -> Atom.TRUE);
		env = env.put(Atom.FALSE.getName(), () -> Atom.FALSE);

		env = env.put(TermOp.EQUAL_.getName(), equal);
		env = env.put(TermOp.NOTEQ_.getName(), noteq);
		env = env.put(TermOp.PLUS__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) + i(b)))));
		env = env.put(TermOp.MINUS_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) - i(b)))));
		env = env.put(TermOp.MULT__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) * i(b)))));
		env = env.put(TermOp.DIVIDE.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) / i(b)))));

		env = env.put(ERROR.getName(), error);
		env = env.put(FST__.getName(), () -> new Fun_(in -> ((Pair_) in.source()).first));
		env = env.put(SND__.getName(), () -> new Fun_(in -> ((Pair_) in.source()).second));
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
				result = memoize(() -> new Pair_(lazy(lhs, env), lazy(rhs, env)));
			else if (operator == TermOp.BRACES) // a {b}
				result = ((Fun_) lazy(lhs, env).source()).fun.apply(lazy(rhs, env));
			else if (operator == TermOp.CONTD_) { // a := b >> c
				@SuppressWarnings("unchecked")
				Source<Node> val[] = (Source<Node>[]) new Source<?>[] { null };
				IMap<String, Source<Node>> env1 = env.put(v(l(lhs)), () -> val[0].source());
				val[0] = lazy(r(lhs), env1)::source;
				result = lazy(rhs, env1);
			} else if (operator == TermOp.FUN___) // a => b
				result = memoize(() -> new Fun_(in -> lazy(rhs, env.put(v(lhs), in))));
			else if (operator == TermOp.TUPLE_) // if a then b else c
				result = lazy(b(lazy(l(rhs), env)) ? l(r(r(rhs))) : r(r(r(r(rhs)))), env);
			else {
				Source<Node> r0 = env.get(operator.getName());
				Source<Node> r1 = ((Fun_) r0.source()).fun.apply(lazy(lhs, env));
				Source<Node> r2 = ((Fun_) r1.source()).fun.apply(lazy(rhs, env));
				result = r2;
			}
		} else if (node instanceof Atom)
			if ((result = env.get(v(node))) == null)
				throw new RuntimeException("Cannot resolve " + node);
			else
				;
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

	private int i(Source<Node> source) {
		return ((Int) source.source()).getNumber();
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
