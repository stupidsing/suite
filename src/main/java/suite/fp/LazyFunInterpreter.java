package suite.fp;

import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;

public class LazyFunInterpreter {

	private Atom ERROR = Atom.of("error");
	private Atom FST__ = Atom.of("fst");
	private Atom SND__ = Atom.of("snd");

	public interface Thunk_ {
		public Node source();
	}

	private static class Fun_ extends Node {
		private Fun<Thunk_, Thunk_> fun;

		private Fun_(Fun<Thunk_, Thunk_> fun) {
			this.fun = fun;
		}
	}

	private static class Pair_ extends Node {
		private Thunk_ first;
		private Thunk_ second;

		private Pair_(Thunk_ left, Thunk_ right) {
			this.first = left;
			this.second = right;
		}
	}

	public Thunk_ lazy(Node node) {
		Thunk_ equal = () -> new Fun_(a -> () -> new Fun_(b -> () -> i(a) == i(b) ? Atom.TRUE : Atom.FALSE));
		Thunk_ noteq = () -> new Fun_(a -> () -> new Fun_(b -> () -> i(a) != i(b) ? Atom.TRUE : Atom.FALSE));
		Thunk_ error = () -> {
			throw new RuntimeException("Error termination");
		};

		IMap<String, Thunk_> env = new IMap<>();
		env = env.put(Atom.TRUE.getName(), () -> Atom.TRUE);
		env = env.put(Atom.FALSE.getName(), () -> Atom.FALSE);

		env = env.put(TermOp.EQUAL_.getName(), equal);
		env = env.put(TermOp.NOTEQ_.getName(), noteq);
		env = env.put(TermOp.PLUS__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) + i(b)))));
		env = env.put(TermOp.MINUS_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) - i(b)))));
		env = env.put(TermOp.MULT__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) * i(b)))));
		env = env.put(TermOp.DIVIDE.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) / i(b)))));
		env = env.put(TermOp.AND___.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> new Pair_(a, b))));

		env = env.put(ERROR.getName(), error);
		env = env.put(FST__.getName(), () -> new Fun_(in -> ((Pair_) in.source()).first));
		env = env.put(SND__.getName(), () -> new Fun_(in -> ((Pair_) in.source()).second));

		return lazy0(node).apply(env);
	}

	private Fun<IMap<String, Thunk_>, Thunk_> lazy0(Node node) {
		Fun<IMap<String, Thunk_>, Thunk_> result;
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator operator = tree.getOperator();
			Node lhs = tree.getLeft();
			Node rhs = tree.getRight();

			if (operator == TermOp.BRACES) { // fun {param}
				Fun<IMap<String, Thunk_>, Thunk_> fun = lazy0(lhs);
				Fun<IMap<String, Thunk_>, Thunk_> param = lazy0(rhs);
				result = env -> ((Fun_) fun.apply(env).source()).fun.apply(param.apply(env));
			} else if (operator == TermOp.CONTD_) { // key := value >> expr
				Fun<IMap<String, Thunk_>, Thunk_> value = lazy0(r(lhs));
				Fun<IMap<String, Thunk_>, Thunk_> expr = lazy0(rhs);
				result = env -> {
					Thunk_ val[] = new Thunk_[] { null };
					IMap<String, Thunk_> env1 = env.put(v(l(lhs)), () -> val[0].source());
					val[0] = value.apply(env1)::source;
					return expr.apply(env1);
				};
			} else if (operator == TermOp.FUN___) { // var => value
				Fun<IMap<String, Thunk_>, Thunk_> value = lazy0(rhs);
				result = env -> () -> new Fun_(in -> value.apply(env.put(v(lhs), in)));
			} else if (operator == TermOp.TUPLE_) { // if a then b else c
				Fun<IMap<String, Thunk_>, Thunk_> if_ = lazy0(l(rhs));
				Fun<IMap<String, Thunk_>, Thunk_> then_ = lazy0(l(r(r(rhs))));
				Fun<IMap<String, Thunk_>, Thunk_> else_ = lazy0(r(r(r(r(rhs)))));
				result = env -> (b(if_.apply(env)) ? then_ : else_).apply(env);
			} else {
				Fun<IMap<String, Thunk_>, Thunk_> p0 = lazy0(lhs);
				Fun<IMap<String, Thunk_>, Thunk_> p1 = lazy0(rhs);
				result = env -> {
					Thunk_ r0 = env.get(operator.getName());
					Thunk_ r1 = ((Fun_) r0.source()).fun.apply(p0.apply(env));
					Thunk_ r2 = ((Fun_) r1.source()).fun.apply(p1.apply(env));
					return r2;
				};
			}
		} else if (node instanceof Atom)
			result = env -> env.get(v(node));
		else
			result = env -> () -> node;

		return result;
	}

	private boolean b(Thunk_ node) {
		return node.source() == Atom.TRUE;
	}

	private int i(Thunk_ source) {
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
