package suite.fp;

import java.util.Map.Entry;

import suite.Suite;
import suite.adt.Mutable;
import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.node.util.TreeUtil.IntInt_Bool;
import suite.primitive.IntInt_Int;
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

		IMap<String, Thunk_> env = IMap.empty();
		env = env.put(Atom.TRUE.name, () -> Atom.TRUE);
		env = env.put(Atom.FALSE.name, () -> Atom.FALSE);

		env = env.put(TermOp.AND___.name, () -> new Fun_(a -> () -> new Fun_(b -> () -> new Pair_(a, b))));
		env = env.put(ERROR.name, error);
		env = env.put(FST__.name, () -> new Fun_(in -> ((Pair_) in.get()).first));
		env = env.put(SND__.name, () -> new Fun_(in -> ((Pair_) in.get()).second));

		for (Entry<Operator, IntInt_Bool> e : TreeUtil.boolOperations.entrySet()) {
			IntInt_Bool fun = e.getValue();
			env = env.put(e.getKey().getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(fun.apply(i(a), i(b))))));
		}

		for (Entry<Operator, IntInt_Int> e : TreeUtil.intOperations.entrySet()) {
			IntInt_Int fun = e.getValue();
			env = env.put(e.getKey().getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(fun.apply(i(a), i(b))))));
		}

		return lazy_(node).apply(env);
	}

	private Fun<IMap<String, Thunk_>, Thunk_> lazy_(Node node) {
		Fun<IMap<String, Thunk_>, Thunk_> result;
		Tree tree;
		Node[] m;

		if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null) {
			String vk = v(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> value = lazy_(m[1]);
			Fun<IMap<String, Thunk_>, Thunk_> expr = lazy_(m[2]);
			result = env -> {
				Mutable<Thunk_> val = Mutable.nil();
				IMap<String, Thunk_> env1 = env.put(vk, () -> val.get().get());
				val.set(value.apply(env1)::get);
				return expr.apply(env1);
			};
		} else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null) {
			Fun<IMap<String, Thunk_>, Thunk_> if_ = lazy_(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> then_ = lazy_(m[1]);
			Fun<IMap<String, Thunk_>, Thunk_> else_ = lazy_(m[2]);
			result = env -> (if_.apply(env).get() == Atom.TRUE ? then_ : else_).apply(env);
		} else if ((m = Suite.match(".0 => .1").apply(node)) != null) {
			String vk = v(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> value = lazy_(m[1]);
			result = env -> () -> new Fun_(in -> value.apply(env.put(vk, in)));
		} else if ((m = Suite.match(".0 {.1}").apply(node)) != null) {
			Fun<IMap<String, Thunk_>, Thunk_> fun = lazy_(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> param = lazy_(m[1]);
			result = env -> fun(fun.apply(env).get()).apply(param.apply(env));
		} else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			Fun<IMap<String, Thunk_>, Thunk_> p0 = lazy_(tree.getLeft());
			Fun<IMap<String, Thunk_>, Thunk_> p1 = lazy_(tree.getRight());
			result = env -> {
				Thunk_ r0 = env.get(operator.getName());
				Thunk_ r1 = fun(r0.get()).apply(p0.apply(env));
				Thunk_ r2 = fun(r1.get()).apply(p1.apply(env));
				return r2;
			};
		} else if (node instanceof Atom) {
			String vk = v(node);
			result = env -> env.get(vk);
		} else
			result = env -> () -> node;

		return result;
	}

	private Iterate<Thunk_> fun(Node n) {
		return ((Fun_) n).fun;
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Thunk_ thunk) {
		return ((Int) thunk.get()).number;
	}

	private String v(Node node) {
		return ((Atom) node).name;
	}

}
