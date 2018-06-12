package suite.fp;

import suite.adt.Mutable;
import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
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

	private static class Cons extends Node {
		private Thunk fst;
		private Thunk snd;

		private Cons(Thunk fst, Thunk snd) {
			this.fst = fst;
			this.snd = snd;
		}
	}

	public Thunk lazy(Node node) {
		Thunk error = () -> Fail.t("error termination");

		var env0 = IMap //
				.<String, Thunk> empty() //
				.put(Atom.TRUE.name, () -> Atom.TRUE) //
				.put(Atom.FALSE.name, () -> Atom.FALSE) //
				.put(TermOp.AND___.name, () -> new Fn(a -> () -> new Fn(b -> () -> new Cons(a, b)))) //
				.put(ERROR.name, error) //
				.put(FST__.name, () -> new Fn(in -> ((Cons) in.get()).fst)) //
				.put(SND__.name, () -> new Fn(in -> ((Cons) in.get()).snd));

		var env1 = Read //
				.from2(TreeUtil.boolOperations) //
				.fold(env0, (e, k, f) -> e.put(k.name_(), () -> new Fn(a -> () -> new Fn(b -> () -> b(f.apply(i(a), i(b)))))));

		var env2 = Read //
				.from2(TreeUtil.intOperations) //
				.fold(env1, (e, k, f) -> e.put(k.name_(), () -> new Fn(a -> () -> new Fn(b -> () -> i(f.apply(i(a), i(b)))))));

		return lazy0(node).apply(env2);
	}

	private Fun<IMap<String, Thunk>, Thunk> lazy0(Node node) {
		return new SwitchNode<Fun<IMap<String, Thunk>, Thunk>>(node //
		).match3("define .0 := .1 >> .2", (a, b, c) -> {
			var vk = v(a);
			var value = lazy0(b);
			var expr = lazy0(c);
			return env -> {
				var val = Mutable.<Thunk> nil();
				var env1 = env.put(vk, () -> val.get().get());
				val.set(() -> value.apply(env1).get());
				return expr.apply(env1);
			};
		}).match3("if .0 then .1 else .2", (a, b, c) -> {
			var if_ = lazy0(a);
			var then_ = lazy0(b);
			var else_ = lazy0(c);
			return env -> () -> (if_.apply(env).get() == Atom.TRUE ? then_ : else_).apply(env).get();
		}).match2(".0 => .1", (a, b) -> {
			var vk = v(a);
			var value = lazy0(b);
			return env -> () -> new Fn(in -> () -> value.apply(env.put(vk, in)).get());
		}).match2(".0 {.1}", (a, b) -> {
			var fun = lazy0(a);
			var param = lazy0(b);
			return env -> () -> fun(fun.apply(env)).apply(param.apply(env)).get();
		}).applyTree((op, l, r) -> {
			var p0 = lazy0(l);
			var p1 = lazy0(r);
			return env -> {
				var r0 = env.get(op.name_());
				var r1 = fun(r0).apply(p0.apply(env));
				var r2 = fun(r1).apply(p1.apply(env));
				return r2;
			};
		}).applyIf(Atom.class, a -> {
			var vk = v(node);
			return env -> env.get(vk);
		}).applyIf(Node.class, a -> {
			return env -> () -> node;
		}).result();
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
