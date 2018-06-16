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
	}

	private static class Cons extends Node {
		private Thunk fst;
		private Thunk snd;
	}

	private static class Frame extends ArrayList<Thunk> {
		private static final long serialVersionUID = 1l;
		private Frame parent;
	}

	public Thunk lazy(Node node) {
		var parsed = parse(node);
		inferType(parsed);

		var boolOpMap = Read //
				.from2(TreeUtil.boolOperations) //
				.<String, Thunk> map2((k, fun) -> k.name_(), (k, fun) -> bi((a, b) -> b(fun.apply(compare(a.get(), b.get()), 0)))) //
				.toMap();

		var intOpMap = Read //
				.from2(TreeUtil.intOperations) //
				.<String, Thunk> map2((k, fun) -> k.name_(), (k, fun) -> bi((a, b) -> Int.of(fun.apply(i(a), i(b))))) //
				.toMap();

		var df = new HashMap<String, Thunk>();
		df.put(TermOp.AND___.name, bi(this::cons));
		df.put("fst", () -> f(in -> ((Cons) in.get()).fst));
		df.put("if", () -> f(a -> () -> f(b -> () -> f(c -> a.get() == Atom.TRUE ? b : c))));
		df.put("snd", () -> f(in -> ((Cons) in.get()).snd));
		df.putAll(boolOpMap);
		df.putAll(intOpMap);

		var keys = df.keySet().stream().sorted().collect(Collectors.toList());
		var lazy0 = new Lazy(0, IMap.empty());
		var frame = new Frame();

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

		class InferType {
			private IMap<String, Node> env;

			private InferType(IMap<String, Node> env) {
				this.env = env;
			}

			private Node infer(Node node) {
				return new SwitchNode<Node>(node //
				).match(Matcher.apply, (param, fun) -> {
					var tr = new Reference();
					bind(Suite.substitute("FUN .0 .1", infer(param), tr), infer(fun));
					return tr;
				}).match(Matcher.atom, value -> {
					return Suite.parse("ATOM");
				}).match(Matcher.boolean_, value -> {
					return Suite.parse("BOOLEAN");
				}).match(Matcher.chars, value -> {
					return Suite.parse("CHARS");
				}).match(Matcher.cons, (type, head, tail) -> {
					return Suite.substitute("CONS .0 .1", infer(head), infer(tail));
				}).match(Matcher.decons, (type, value, left, right, then, else_) -> {
					var t0 = new Reference();
					var t1 = new Reference();
					var tr = infer(else_);
					var i1 = new InferType(env.put(Atom.name(left), t0).put(Atom.name(right), t1));
					bind(Suite.substitute("CONS .0 .1", t0, t1), infer(value));
					bind(tr, i1.infer(then));
					return tr;
				}).match(Matcher.defvars, (list, do_) -> {
					var tuple = Suite.pattern(".0 .1");
					var defs = Tree.iter(list).map(tuple::match).map2(a -> Atom.name(a[0]), a -> a[1]).collect();
					var env1 = defs.keys().fold(env, (e, v) -> e.replace(v, new Reference()));
					var i1 = new InferType(env1);
					defs.sink((v, def) -> bind(i1.infer(def), env1.get(v)));
					return i1.infer(do_);
				}).match(Matcher.error, m -> {
					return new Reference();
				}).match(Matcher.fun, (param, do_) -> {
					var tp = new Reference();
					var env1 = env.replace(Atom.name(param), tp);
					return Suite.substitute("FUN .0 .1", tp, new InferType(env1).infer(do_));
				}).match(Matcher.if_, (if_, then_, else_) -> {
					var tr = new Reference();
					bind(Suite.parse("BOOLEAN"), infer(if_));
					bind(tr, infer(then_));
					bind(tr, infer(else_));
					return tr;
				}).match(Matcher.nil, () -> {
					return Suite.substitute("NIL .0", new Reference());
				}).match(Matcher.number, value -> {
					return Suite.substitute("NUMBER");
				}).match(Matcher.pragma, do_ -> {
					return infer(do_);
				}).match(Matcher.tco, (iter, in_) -> {
					var tv = infer(in_);
					var tr = new Reference();
					var tl = Suite.substitute("FUN .0 CONS BOOLEAN CONS .1 .2", tv, tv, tr);
					bind(tl, infer(iter));
					return tr;
				}).match(Matcher.tree, (op, left, right) -> {
					var tr = new Reference();
					var tl = Suite.substitute("FUN .0 FUN .1 .2", infer(left), infer(right), tr);
					bind(tl, get(op));
					return tr;
				}).match(Matcher.unwrap, do_ -> {
					return infer(do_);
				}).match(Matcher.var, name -> {
					return get(name);
					// return new Cloner().clone(get(name));
				}).match(Matcher.wrap, do_ -> {
					return infer(do_);
				}).nonNullResult();
			}

			private boolean bind(Node t0, Node t1) {
				return Binder.bind(t0, t1, new Trail()) ? true : Fail.t();
			}

			private Node get(Node var) {
				return env.get(Atom.name(var));
			}
		}

		return new InferType(env2).infer(node);
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
			).match(Matcher.apply, (param, fun) -> {
				var param_ = lazy(param);
				var fun_ = lazy(fun);
				return frame -> {
					return () -> fun(fun_.apply(frame).get()).apply(param_.apply(frame)).get();
				};
			}).match(Matcher.atom, value -> {
				return immediate(value);
			}).match(Matcher.boolean_, value -> {
				return immediate(value);
			}).match(Matcher.chars, value -> {
				return immediate(new Data<>(To.chars(Str.str(value))));
			}).match(Matcher.cons, (type, head, tail) -> {
				var p0_ = lazy(head);
				var p1_ = lazy(tail);
				return frame -> () -> cons(p0_.apply(frame), p1_.apply(frame));
			}).match(Matcher.decons, (type, v, l, r, th, el) -> {
				var value_ = lazy(v);
				var then_ = put(l).put(r).lazy(th);
				var else_ = lazy(el);

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
			}).match("DEF-VARS (.0 .1,) .2", (a, b, c) -> {
				var lazy1 = put(a);
				var value_ = lazy1.lazy(b);
				var expr = lazy1.lazy(c);

				return frame -> {
					var value = Mutable.<Thunk> nil();
					frame.add(() -> value.get().get());
					value.set(() -> value_.apply(frame).get());
					return expr.apply(frame);
				};
			}).match(Matcher.defvars, (list, do_) -> {
				var tuple = Suite.pattern(".0 .1");
				var arrays = Tree.iter(list).map(tuple::match).collect();
				var size = arrays.size();
				var lazy = arrays.fold(this, (l, array) -> l.put(array[0]));
				var values_ = arrays.map(array -> lazy.lazy(array[1])).toList();
				var expr = lazy.lazy(do_);

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
			}).match(Matcher.error, m -> {
				return frame -> () -> Fail.t("error termination " + Formatter.display(m));
			}).match(Matcher.fun, (param, do_) -> {
				var vm1 = IMap.<Node, Fun<Frame, Thunk>> empty();
				for (var e : vm) {
					var getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}
				var value_ = new Lazy(0, vm1).put(param).lazy(do_);
				return frame -> () -> f(in -> {
					var frame1 = new Frame();
					frame1.parent = frame;
					frame1.add(in);
					return value_.apply(frame1);
				});
			}).match(Matcher.if_, (if_, then, else_) -> {
				return lazy(Suite.substitute("APPLY .2 APPLY .1 APPLY .0 VAR if", if_, then, else_));
			}).match(Matcher.nil, () -> {
				return immediate(Atom.NIL);
			}).match(Matcher.number, value -> {
				return immediate(value);
			}).match(Matcher.pragma, do_ -> {
				return lazy(do_);
			}).match(Matcher.tco, (it, i) -> {
				var iter_ = lazy(it);
				var in_ = lazy(i);
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
			}).match(Matcher.tree, (op, left, right) -> {
				return lazy(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", op, left, right));
			}).match(Matcher.unwrap, do_ -> {
				return lazy(do_);
			}).match(Matcher.var, name -> {
				return vm.get(name);
			}).match(Matcher.wrap, do_ -> {
				return lazy(do_);
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
		return () -> f(a -> () -> f(b -> () -> fun.apply(a, b)));
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

	private int i(Thunk thunk) {
		return Int.num(thunk.get());
	}

}
