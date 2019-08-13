package suite.fp;

import static primal.statics.Fail.fail;

import primal.MoreVerbs.Read;
import primal.adt.Mutable;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.persistent.PerMap;
import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;

public class InterpretFunLazy0 {

	private Atom ERROR = Atom.of("error");
	private Atom FST__ = Atom.of("fst");
	private Atom SND__ = Atom.of("snd");

	public interface Thunk {
		public Node get();
	}

	private static class Fn extends Node {
		private Iterate<Thunk> fun;
	}

	private static class Cons extends Node {
		private Thunk fst;
		private Thunk snd;
	}

	public Node inferType(Node node) {
		class InferType {
			private PerMap<String, Node> env;

			private InferType(PerMap<String, Node> env) {
				this.env = env;
			}

			private Node infer(Node node) {
				return new SwitchNode<Node>(node //
				).match("define .0 := .1 ~ .2", (a, b, c) -> {
					var tv = new Reference();
					var i1 = new InferType(env.put(Atom.name(a), tv));
					bind(infer(b), tv);
					return i1.infer(c);
				}).match("if .0 then .1 else .2", (a, b, c) -> {
					var tr = new Reference();
					bind(Suite.parse("BOOLEAN"), infer(a));
					bind(tr, infer(b));
					bind(tr, infer(c));
					return tr;
				}).match(".0 => .1", (a, b) -> {
					var tp = new Reference();
					var env1 = env.replace(Atom.name(a), tp);
					return Suite.substitute("FUN .0 .1", tp, new InferType(env1).infer(b));
				}).match(".0_{.1}", (a, b) -> {
					var tr = new Reference();
					bind(Suite.substitute("FUN .0 .1", infer(b), tr), infer(a));
					return tr;
				}).applyTree((op, l, r) -> {
					var tr = new Reference();
					var tl = Suite.substitute("FUN .0 FUN .1 .2", infer(l), infer(r), tr);
					bind(tl, env.get(op.name_()));
					return tr;
				}).applyIf(Atom.class, a -> {
					return env.get(a.name);
				}).applyIf(Int.class, a -> {
					return Suite.parse("NUMBER");
				}).applyIf(Node.class, a -> {
					return Atom.NIL;
				}).nonNullResult();
			}

			private boolean bind(Node t0, Node t1) {
				return Binder.bind(t0, t1, new Trail()) ? true : fail();
			}
		}

		var env0 = PerMap //
				.<String, Node> empty() //
				.put(Atom.TRUE.name, Suite.parse("BOOLEAN")) //
				.put(Atom.FALSE.name, Suite.parse("BOOLEAN")) //
				.put(TermOp.AND___.name, Suite.substitute("FUN .0 FUN .1 CONS .0 .1")) //
				.put(ERROR.name, new Reference()) //
				.put(FST__.name, Suite.substitute("FUN (CONS .0 .1) .0")) //
				.put(SND__.name, Suite.substitute("FUN (CONS .0 .1) .1"));

		var env1 = Read //
				.from2(TreeUtil.boolOperations) //
				.keys() //
				.fold(env0, (e, o) -> e.put(o.name_(), Suite.substitute("FUN NUMBER FUN NUMBER BOOLEAN")));

		var env2 = Read //
				.from2(TreeUtil.intOperations) //
				.keys() //
				.fold(env1, (e, o) -> e.put(o.name_(), Suite.substitute("FUN NUMBER FUN NUMBER NUMBER")));

		return new InferType(env2).infer(node);
	}

	public Thunk lazy(Node node) {
		Thunk error = () -> fail("error termination");

		var env0 = PerMap //
				.<String, Thunk> empty() //
				.put(Atom.TRUE.name, () -> Atom.TRUE) //
				.put(Atom.FALSE.name, () -> Atom.FALSE) //
				.put(TermOp.AND___.name, () -> f(a -> () -> f(b -> () -> cons(a, b)))) //
				.put(ERROR.name, error) //
				.put(FST__.name, () -> f(in -> ((Cons) in.get()).fst)) //
				.put(SND__.name, () -> f(in -> ((Cons) in.get()).snd));

		var env1 = Read //
				.from2(TreeUtil.boolOperations) //
				.fold(env0, (e, k, f) -> e.put(k.name_(), () -> f(a -> () -> f(b -> () -> b(f.apply(i(a), i(b)))))));

		var env2 = Read //
				.from2(TreeUtil.intOperations) //
				.fold(env1, (e, k, f) -> e.put(k.name_(), () -> f(a -> () -> f(b -> () -> i(f.apply(i(a), i(b)))))));

		return lazy0(node).apply(env2);
	}

	private Fun<PerMap<String, Thunk>, Thunk> lazy0(Node node) {
		return new SwitchNode<Fun<PerMap<String, Thunk>, Thunk>>(node //
		).match("define .0 := .1 ~ .2", (a, b, c) -> {
			var vk = Atom.name(a);
			var value = lazy0(b);
			var expr = lazy0(c);
			return env -> {
				var val = Mutable.<Thunk> nil();
				var env1 = env.put(vk, () -> val.value().get());
				val.set(() -> value.apply(env1).get());
				return expr.apply(env1);
			};
		}).match("if .0 then .1 else .2", (a, b, c) -> {
			var if_ = lazy0(a);
			var then_ = lazy0(b);
			var else_ = lazy0(c);
			return env -> () -> (if_.apply(env).get() == Atom.TRUE ? then_ : else_).apply(env).get();
		}).match(".0 => .1", (a, b) -> {
			var vk = Atom.name(a);
			var value = lazy0(b);
			return env -> () -> f(in -> () -> value.apply(env.put(vk, in)).get());
		}).match(".0_{.1}", (a, b) -> {
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
			var vk = a.name;
			return env -> env.get(vk);
		}).applyIf(Node.class, a -> {
			return env -> () -> node;
		}).result();
	}

	private Node b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private Cons cons(Thunk fst, Thunk snd) {
		var cons = new Cons();
		cons.fst = fst;
		cons.snd = snd;
		return cons;
	}

	private Node f(Iterate<Thunk> fun) {
		var fn = new Fn();
		fn.fun = fun;
		return fn;
	}

	private Node i(int i) {
		return Int.of(i);
	}

	private Iterate<Thunk> fun(Thunk n) {
		return ((Fn) n.get()).fun;
	}

	private int i(Thunk thunk) {
		return Int.num(thunk.get());
	}

}
