package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import suite.Suite;
import suite.adt.Mutable;
import suite.immutable.IMap;
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
import suite.streamlet.As;
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

	private static class Pair extends Node {
		private Thunk fst;
		private Thunk snd;

		private Pair(Thunk fst, Thunk snd) {
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
		var df = new HashMap<String, Thunk>();

		df.put(TermOp.AND___.name, bi((a, b) -> new Pair(a, b)));
		df.put("fst", () -> new Fn(in -> ((Pair) in.get()).fst));
		df.put("if", () -> new Fn(a -> () -> new Fn(b -> () -> new Fn(c -> a.get() == Atom.TRUE ? b : c))));
		df.put("snd", () -> new Fn(in -> ((Pair) in.get()).snd));

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
				return frame -> () -> new Pair(p0_.apply(frame), p1_.apply(frame));
			}).match(Matcher.decons, DECONS -> {
				var value_ = lazy(DECONS.value);
				var then_ = put(DECONS.left).put(DECONS.right).lazy(DECONS.then);
				var else_ = lazy(DECONS.else_);

				return frame -> {
					var value = value_.apply(frame).get();
					if (value instanceof Pair) {
						var pair = (Pair) value;
						frame.add(pair.fst);
						frame.add(pair.snd);
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
				var arrays = Tree.iter(DEFVARS.list).map(tuple::match).collect(As::streamlet);
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
					Pair p0, p1;
					do {
						var out = iter.apply(in);
						p0 = (Pair) out.get();
						p1 = (Pair) p0.snd.get();
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
		var t0 = n0 instanceof Fn ? 2 : (n0 instanceof Pair ? 1 : 0);
		var t1 = n1 instanceof Fn ? 2 : (n0 instanceof Pair ? 1 : 0);
		var c = t0 - t1;
		if (c == 0)
			switch (t0) {
			case 0:
				c = Comparer.comparer.compare(n0, n1);
				break;
			case 1:
				var p0 = (Pair) n0;
				var p1 = (Pair) n1;
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

}
