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

	public interface Thunk_ {
		public Node get();
	}

	private static class Fun_ extends Node {
		private Iterate<Thunk_> fun;

		private Fun_(Iterate<Thunk_> fun) {
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
		Thunk_ error = () -> Fail.t("error termination");

		var env = IMap.<String, Thunk_> empty() //
				.put(Atom.TRUE.name, () -> Atom.TRUE) //
				.put(Atom.FALSE.name, () -> Atom.FALSE) //
				.put(TermOp.AND___.name, () -> new Fun_(a -> () -> new Fun_(b -> () -> new Pair_(a, b)))) //
				.put(ERROR.name, error) //
				.put(FST__.name, () -> new Fun_(in -> ((Pair_) in.get()).first)) //
				.put(SND__.name, () -> new Fun_(in -> ((Pair_) in.get()).second));

		env = Read.from2(TreeUtil.boolOperations).fold(env, (e, k, fun) -> {
			return e.put(k.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(fun.apply(i(a), i(b))))));
		});

		env = Read.from2(TreeUtil.intOperations).fold(env, (e, k, fun) -> {
			return e.put(k.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(fun.apply(i(a), i(b))))));
		});

		return lazy_(node).apply(env);
	}

	private Fun<IMap<String, Thunk_>, Thunk_> lazy_(Node node) {
		Fun<IMap<String, Thunk_>, Thunk_> result;
		Tree tree;
		Node[] m;

		if ((m = Suite.pattern("define .0 := .1 >> .2").match(node)) != null) {
			var vk = v(m[0]);
			var value = lazy_(m[1]);
			var expr = lazy_(m[2]);
			result = env -> {
				var val = Mutable.<Thunk_> nil();
				var env1 = env.put(vk, () -> val.get().get());
				val.set(() -> value.apply(env1).get());
				return expr.apply(env1);
			};
		} else if ((m = Suite.pattern("if .0 then .1 else .2").match(node)) != null) {
			var if_ = lazy_(m[0]);
			var then_ = lazy_(m[1]);
			var else_ = lazy_(m[2]);
			result = env -> () -> (if_.apply(env).get() == Atom.TRUE ? then_ : else_).apply(env).get();
		} else if ((m = Suite.pattern(".0 => .1").match(node)) != null) {
			var vk = v(m[0]);
			var value = lazy_(m[1]);
			result = env -> () -> new Fun_(in -> () -> value.apply(env.put(vk, in)).get());
		} else if ((m = Suite.pattern(".0 {.1}").match(node)) != null) {
			var fun = lazy_(m[0]);
			var param = lazy_(m[1]);
			result = env -> () -> fun(fun.apply(env)).apply(param.apply(env)).get();
		} else if ((tree = Tree.decompose(node)) != null) {
			var operator = tree.getOperator();
			var p0 = lazy_(tree.getLeft());
			var p1 = lazy_(tree.getRight());
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

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private Iterate<Thunk_> fun(Thunk_ n) {
		return ((Fun_) n.get()).fun;
	}

	private int i(Thunk_ thunk) {
		return ((Int) thunk.get()).number;
	}

	private String v(Node node) {
		return ((Atom) node).name;
	}

}
