package suite.fp;

import suite.Suite;
import suite.adt.Mutable;
import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;

public class InterpretFunLazy0 {

	private Atom ERROR = Atom.of("error");
	private Atom FST__ = Atom.of("fst");
	private Atom SND__ = Atom.of("snd");

	public interface Thunk {
		public Node get();
	}

	private static class Fn extends Node {
		private Iterate<Thunk> fun;

		private Fn(Iterate<Thunk> fun) {
			this.fun = fun;
		}
	}

	private static class Pair extends Node {
		private Thunk fst;
		private Thunk snd;

		private Pair(Thunk fst, Thunk snd) {
			this.fst = fst;
			this.snd = snd;
		}
	}

	public Thunk lazy(Node node) {
		Thunk error = () -> Fail.t("error termination");

		var env = IMap.<String, Thunk> empty() //
				.put(Atom.TRUE.name, () -> Atom.TRUE) //
				.put(Atom.FALSE.name, () -> Atom.FALSE) //
				.put(TermOp.AND___.name, () -> new Fn(a -> () -> new Fn(b -> () -> new Pair(a, b)))) //
				.put(ERROR.name, error) //
				.put(FST__.name, () -> new Fn(in -> ((Pair) in.get()).fst)) //
				.put(SND__.name, () -> new Fn(in -> ((Pair) in.get()).snd));

		env = Read //
				.from2(TreeUtil.boolOperations) //
				.fold(env, (e, k, fun) -> e.put(k.getName(), () -> new Fn(a -> () -> new Fn(b -> () -> b(fun.apply(i(a), i(b)))))));

		env = Read //
				.from2(TreeUtil.intOperations) //
				.fold(env, (e, k, fun) -> e.put(k.getName(), () -> new Fn(a -> () -> new Fn(b -> () -> i(fun.apply(i(a), i(b)))))));

		return lazy0(node).apply(env);
	}

	private Fun<IMap<String, Thunk>, Thunk> lazy0(Node node) {
		Fun<IMap<String, Thunk>, Thunk> result;
		Tree tree;
		Node[] m;

		if ((m = Suite.pattern("define .0 := .1 >> .2").match(node)) != null) {
			var vk = v(m[0]);
			var value = lazy0(m[1]);
			var expr = lazy0(m[2]);
			result = env -> {
				var val = Mutable.<Thunk> nil();
				var env1 = env.put(vk, () -> val.get().get());
				val.set(() -> value.apply(env1).get());
				return expr.apply(env1);
			};
		} else if ((m = Suite.pattern("if .0 then .1 else .2").match(node)) != null) {
			var if_ = lazy0(m[0]);
			var then_ = lazy0(m[1]);
			var else_ = lazy0(m[2]);
			result = env -> () -> (if_.apply(env).get() == Atom.TRUE ? then_ : else_).apply(env).get();
		} else if ((m = Suite.pattern(".0 => .1").match(node)) != null) {
			var vk = v(m[0]);
			var value = lazy0(m[1]);
			result = env -> () -> new Fn(in -> () -> value.apply(env.put(vk, in)).get());
		} else if ((m = Suite.pattern(".0 {.1}").match(node)) != null) {
			var fun = lazy0(m[0]);
			var param = lazy0(m[1]);
			result = env -> () -> fun(fun.apply(env)).apply(param.apply(env)).get();
		} else if ((tree = Tree.decompose(node)) != null) {
			var operator = tree.getOperator();
			var p0 = lazy0(tree.getLeft());
			var p1 = lazy0(tree.getRight());
			result = env -> {
				var r0 = env.get(operator.getName());
				var r1 = fun(r0).apply(p0.apply(env));
				var r2 = fun(r1).apply(p1.apply(env));
				return r2;
			};
		} else if (node instanceof Atom) {
			var vk = v(node);
			result = env -> env.get(vk);
		} else
			result = env -> () -> node;

		return result;
	}

	private Node b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private Node i(int i) {
		return Int.of(i);
	}

	private Iterate<Thunk> fun(Thunk n) {
		return ((Fn) n.get()).fun;
	}

	private int i(Thunk thunk) {
		return ((Int) thunk.get()).number;
	}

	private String v(Node node) {
		return ((Atom) node).name;
	}

}
