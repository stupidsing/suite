package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import suite.Suite;
import suite.adt.Mutable;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.BiFun;
import suite.util.To;

public class InterpretFunLazy {

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

	private static class Frame extends ArrayList<Thunk> {
		private static final long serialVersionUID = 1l;
		private Frame parent;

		private Frame(Frame parent) {
			this.parent = parent;
		}
	}

	public Thunk lazy(Node node) {
		var parsed = parse(node);
		inferType(parsed);

		var df = new HashMap<String, Thunk>();

		df.put(TermOp.AND___.name, bi((a, b) -> new Cons(a, b)));
		df.put("fst", () -> new Fn(in -> ((Cons) in.get()).fst));
		df.put("if", () -> new Fn(a -> () -> new Fn(b -> () -> new Fn(c -> a.get() == Atom.TRUE ? b : c))));
		df.put("snd", () -> new Fn(in -> ((Cons) in.get()).snd));

		TreeUtil.boolOperations.forEach((k, fun) -> df.put(k.name_(), bi((a, b) -> b(fun.apply(compare(a.get(), b.get()), 0)))));
		TreeUtil.intOperations.forEach((k, fun) -> df.put(k.name_(), bi((a, b) -> Int.of(fun.apply(i(a), i(b))))));

		var keys = df.keySet().stream().sorted().collect(Collectors.toList());
		var lazy0 = new Lazy(0, IMap.empty());
		var frame = new Frame(null);

		for (var key : keys) {
			lazy0 = lazy0.put(Atom.of(key));
			frame.add(df.get(key));
		}

		return lazy0.lazy(parsed).apply(frame);
	}

	private Reference parse(Node node) {
		var prover = new Prover(Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl")));
		var parsed = new Reference();

		return prover.prove(Suite.substitute("fc-parse .0 .1", node, parsed)) //
				? parsed //
				: Fail.t("cannot parse " + node);
	}

	private Node inferType(Node node) {
		var env0 = IMap //
				.<String, Node> empty() //
				.put(TermOp.AND___.name, Suite.substitute("FUN .0 FUN .1 CONS .0 .1")) //
				.put("fst", Suite.substitute("FUN (CONS .0 .1) .0")) //
				.put("if", Suite.substitute("FUN BOOLEAN FUN .1 FUN .1 .1")) //
				.put("snd", Suite.substitute("FUN (CONS .0 .1) .1"));

		var env1 = Read //
				.from2(TreeUtil.boolOperations) //
				.keys() //
				.fold(env0, (e, o) -> e.put(o.name_(), Suite.substitute("FUN .0 FUN .0 BOOLEAN")));

		var env2 = Read //
				.from2(TreeUtil.intOperations) //
				.keys() //
				.fold(env1, (e, o) -> e.put(o.name_(), Suite.substitute("FUN NUMBER FUN NUMBER NUMBER")));

		return new InferType(env2).infer(node);
	}

	private class InferType {
		private IMap<String, Node> env;

		private InferType(IMap<String, Node> env) {
			this.env = env;
		}

		private Node infer(Node node) {
			return new SwitchNode<Node>(node //
			).match(Matcher.apply, APPLY -> {
				var tr = new Reference();
				bind(Suite.substitute("FUN .0 .1", infer(APPLY.param), tr), infer(APPLY.fun));
				return tr;
			}).match(Matcher.atom, ATOM -> {
				return Suite.parse("ATOM");
			}).match(Matcher.boolean_, BOOLEAN -> {
				return Suite.parse("BOOLEAN");
			}).match(Matcher.chars, CHARS -> {
				return Suite.parse("CHARS");
			}).match(Matcher.cons, CONS -> {
				return Suite.substitute("CONS .0 .1", infer(CONS.head), infer(CONS.tail));
			}).match(Matcher.decons, DECONS -> {
				var t0 = new Reference();
				var t1 = new Reference();
				var tr = infer(DECONS.else_);
				var i1 = new InferType(env.put(v(DECONS.left), t0).put(v(DECONS.right), t1));
				bind(Suite.substitute("CONS .0 .1", t0, t1), infer(DECONS.value));
				bind(tr, i1.infer(DECONS.then));
				return tr;
			}).match(Matcher.defvars, DEFVARS -> {
				var tuple = Suite.pattern(".0 .1");
				var defs = Tree.iter(DEFVARS.list).map(tuple::match).map2(a -> v(a[0]), a -> a[1]).collect();
				var env1 = defs.keys().fold(env, (e, v) -> e.replace(v, new Reference()));
				var i1 = new InferType(env1);
				defs.sink((v, def) -> bind(i1.infer(def), env1.get(v)));
				return i1.infer(DEFVARS.do_);
			}).match(Matcher.error, ERROR -> {
				return new Reference();
			}).match(Matcher.fun, FUN -> {
				var tp = new Reference();
				var env1 = env.replace(v(FUN.param), tp);
				return Suite.substitute("FUN .0 .1", tp, new InferType(env1).infer(FUN.do_));
			}).match(Matcher.if_, IF -> {
				var tr = new Reference();
				bind(Suite.parse("BOOLEAN"), infer(IF.if_));
				bind(tr, infer(IF.then_));
				bind(tr, infer(IF.else_));
				return tr;
			}).match(Matcher.nil, NIL -> {
				return Suite.substitute("NIL .0", new Reference());
			}).match(Matcher.number, NIL -> {
				return Suite.substitute("NUMBER");
			}).match(Matcher.pragma, PRAGMA -> {
				return infer(PRAGMA.do_);
			}).match(Matcher.tco, TCO -> {
				var tv = infer(TCO.in_);
				var tr = new Reference();
				var tl = Suite.substitute("FUN .0 CONS BOOLEAN CONS .1 .2", tv, tv, tr);
				bind(tl, infer(TCO.iter));
				return tr;
			}).match(Matcher.tree, TREE -> {
				var tr = new Reference();
				var tl = Suite.substitute("FUN .0 FUN .1 .2", infer(TREE.left), infer(TREE.right), tr);
				bind(tl, get(TREE.op));
				return tr;
			}).match(Matcher.unwrap, UNWRAP -> {
				return infer(UNWRAP.do_);
			}).match(Matcher.var, VAR -> {
				return get(VAR.name);
				// return new Cloner().clone(get(VAR.name));
			}).match(Matcher.wrap, WRAP -> {
				return infer(WRAP.do_);
			}).nonNullResult();
		}

		private Node get(Node var) {
			return env.get(v(var));
		}

		private void bind(Node t0, Node t1) {
			if (Binder.bind(t0, t1, new Trail()))
				;
			else
				Fail.t();
		}
	}

	private class Lazy {
		private int fs;
		private IMap<Node, Fun<Frame, Thunk>> vm;

		private Lazy(int fs, IMap<Node, Fun<Frame, Thunk>> vm) {
			this.fs = fs;
			this.vm = vm;
		}

		private Fun<Frame, Thunk> lazy(Node node) {
			return new SwitchNode<Fun<Frame, Thunk>>(node //
			).match(Matcher.apply, APPLY -> {
				var param_ = lazy(APPLY.param);
				var fun_ = lazy(APPLY.fun);
				return frame -> {
					var fun = fun_.apply(frame);
					var param = param_.apply(frame);
					return () -> fun(fun.get()).apply(param).get();
				};
			}).match(Matcher.atom, ATOM -> {
				return immediate(ATOM.value);
			}).match(Matcher.boolean_, BOOLEAN -> {
				return immediate(BOOLEAN.value);
			}).match(Matcher.chars, CHARS -> {
				return immediate(new Data<>(To.chars(((Str) CHARS.value).value)));
			}).match(Matcher.cons, CONS -> {
				var p0_ = lazy(CONS.head);
				var p1_ = lazy(CONS.tail);
				return frame -> () -> new Cons(p0_.apply(frame), p1_.apply(frame));
			}).match(Matcher.decons, DECONS -> {
				var value_ = lazy(DECONS.value);
				var then_ = put(DECONS.left).put(DECONS.right).lazy(DECONS.then);
				var else_ = lazy(DECONS.else_);

				return frame -> {
					var value = value_.apply(frame).get();
					if (value instanceof Cons) {
						var cons = (Cons) value;
						frame.add(cons.fst);
						frame.add(cons.snd);
						return then_.apply(frame);
					} else
						return else_.apply(frame);
				};
			}).match3("DEF-VARS (.0 .1,) .2", (a, b, c) -> {
				var lazy1 = put(a);
				var value_ = lazy1.lazy(b);
				var expr = lazy1.lazy(c);

				return frame -> {
					var value = Mutable.<Thunk> nil();
					frame.add(() -> value.get().get());
					value.set(() -> value_.apply(frame).get());
					return expr.apply(frame);
				};
			}).match(Matcher.defvars, DEFVARS -> {
				var tuple = Suite.pattern(".0 .1");
				var arrays = Tree.iter(DEFVARS.list).map(tuple::match).collect();
				var size = arrays.size();
				var lazy = arrays.fold(this, (l, array) -> l.put(array[0]));
				var values_ = arrays.map(array -> lazy.lazy(array[1])).toList();
				var expr = lazy.lazy(DEFVARS.do_);

				return frame -> {
					var values = new ArrayList<Thunk>(size);
					for (var i = 0; i < size; i++) {
						var i1 = i;
						frame.add(() -> values.get(i1).get());
					}
					for (var value_ : values_) {
						var v_ = value_;
						values.add(() -> v_.apply(frame).get());
					}
					return expr.apply(frame);
				};
			}).match(Matcher.error, ERROR -> {
				return frame -> () -> Fail.t("error termination " + Formatter.display(ERROR.m));
			}).match(Matcher.fun, FUN -> {
				var vm1 = IMap.<Node, Fun<Frame, Thunk>> empty();
				for (var e : vm) {
					var getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}
				var value_ = new Lazy(0, vm1).put(FUN.param).lazy(FUN.do_);
				return frame -> () -> new Fn(in -> {
					var frame1 = new Frame(frame);
					frame1.add(in);
					return value_.apply(frame1);
				});
			}).match(Matcher.if_, IF -> {
				return lazy(Suite.substitute("APPLY .2 APPLY .1 APPLY .0 VAR if", IF.if_, IF.then_, IF.else_));
			}).match(Matcher.nil, NIL -> {
				return immediate(Atom.NIL);
			}).match(Matcher.number, NUMBER -> {
				return immediate(NUMBER.value);
			}).match(Matcher.pragma, PRAGMA -> {
				return lazy(PRAGMA.do_);
			}).match(Matcher.tco, TCO -> {
				var iter_ = lazy(TCO.iter);
				var in_ = lazy(TCO.in_);
				return frame -> {
					var iter = fun(iter_.apply(frame).get());
					var in = in_.apply(frame);
					Cons p0, p1;
					do {
						var out = iter.apply(in);
						p0 = (Cons) out.get();
						p1 = (Cons) p0.snd.get();
						in = p1.fst;
					} while (p0.fst.get() != Atom.TRUE);
					return p1.snd;
				};
			}).match(Matcher.tree, TREE -> {
				return lazy(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", TREE.op, TREE.left, TREE.right));
			}).match(Matcher.unwrap, UNWRAP -> {
				return lazy(UNWRAP.do_);
			}).match(Matcher.var, VAR -> {
				return vm.get(VAR.name);
			}).match(Matcher.wrap, WRAP -> {
				return lazy(WRAP.do_);
			}).nonNullResult();
		}

		private Lazy put(Node node) {
			return new Lazy(fs + 1, vm.put(node, getter(fs)));
		}
	}

	private Fun<Frame, Thunk> getter(int p) {
		return frame -> frame.get(p);
	}

	private Iterate<Thunk> fun(Node n) {
		return ((Fn) n).fun;
	}

	private Fun<Frame, Thunk> immediate(Node n) {
		return frame -> () -> n;
	}

	private int compare(Node n0, Node n1) {
		var t0 = n0 instanceof Fn ? 2 : (n0 instanceof Cons ? 1 : 0);
		var t1 = n1 instanceof Fn ? 2 : (n0 instanceof Cons ? 1 : 0);
		var c = t0 - t1;
		if (c == 0)
			switch (t0) {
			case 0:
				c = Comparer.comparer.compare(n0, n1);
				break;
			case 1:
				var p0 = (Cons) n0;
				var p1 = (Cons) n1;
				c = c == 0 ? compare(p0.fst.get(), p1.fst.get()) : c;
				c = c == 0 ? compare(p0.snd.get(), p1.snd.get()) : c;
				break;
			case 2:
				c = System.identityHashCode(t0) - System.identityHashCode(t1);
			}
		return c;
	}

	private Thunk bi(BiFun<Thunk, Node> fun) {
		return () -> new Fn(a -> () -> new Fn(b -> () -> fun.apply(a, b)));
	}

	private Node b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Thunk thunk) {
		return ((Int) thunk.get()).number;
	}

	private String v(Node node) {
		return ((Atom) node).name;
	}

}
