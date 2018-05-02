package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import suite.Suite;
import suite.adt.Mutable;
import suite.fp.match.Matcher;
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
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.BiFun;
import suite.util.To;

public class InterpretFunLazy {

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
		private Thunk_ first_;
		private Thunk_ second;

		private Pair_(Thunk_ first_, Thunk_ second) {
			this.first_ = first_;
			this.second = second;
		}
	}

	private static class Frame extends ArrayList<Thunk_> {
		private static final long serialVersionUID = 1l;
		private Frame parent;

		private Frame(Frame parent) {
			this.parent = parent;
		}
	}

	public Thunk_ lazy(Node node) {
		var parsed = parse(node);
		var df = new HashMap<String, Thunk_>();

		df.put(TermOp.AND___.name, bi((a, b) -> new Pair_(a, b)));
		df.put("fst", () -> new Fun_(in -> ((Pair_) in.get()).first_));
		df.put("if", () -> new Fun_(a -> () -> new Fun_(b -> () -> new Fun_(c -> a.get() == Atom.TRUE ? b : c))));
		df.put("snd", () -> new Fun_(in -> ((Pair_) in.get()).second));

		TreeUtil.boolOperations.forEach((k, fun) -> df.put(k.getName(), bi((a, b) -> b(fun.apply(compare(a.get(), b.get()), 0)))));
		TreeUtil.intOperations.forEach((k, fun) -> df.put(k.getName(), bi((a, b) -> Int.of(fun.apply(i(a), i(b))))));

		var keys = df.keySet().stream().sorted().collect(Collectors.toList());
		var lazy0 = new Lazy_(0, IMap.empty());
		var frame = new Frame(null);

		for (var key : keys) {
			lazy0 = lazy0.put(Atom.of(key));
			frame.add(df.get(key));
		}

		return lazy0.lazy_(parsed).apply(frame);
	}

	private Reference parse(Node node) {
		var prover = new Prover(Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl")));
		var parsed = new Reference();

		if (prover.prove(Suite.substitute("fc-parse .0 .1", node, parsed)))
			return parsed;
		else
			return Fail.t("cannot parse " + node);
	}

	private class Lazy_ {
		private int fs;
		private IMap<Node, Fun<Frame, Thunk_>> vm;

		private Lazy_(int fs, IMap<Node, Fun<Frame, Thunk_>> vm) {
			this.fs = fs;
			this.vm = vm;
		}

		private Fun<Frame, Thunk_> lazy_(Node node) {
			return new SwitchNode<Fun<Frame, Thunk_>>(node //
			).match(Matcher.apply, APPLY -> {
				var param_ = lazy_(APPLY.param);
				var fun_ = lazy_(APPLY.fun);
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
				var p0_ = lazy_(CONS.head);
				var p1_ = lazy_(CONS.tail);
				return frame -> () -> new Pair_(p0_.apply(frame), p1_.apply(frame));
			}).match(Matcher.decons, DECONS -> {
				var value_ = lazy_(DECONS.value);
				var then_ = put(DECONS.left).put(DECONS.right).lazy_(DECONS.then);
				var else_ = lazy_(DECONS.else_);

				return frame -> {
					var value = value_.apply(frame).get();
					if (value instanceof Pair_) {
						var pair = (Pair_) value;
						frame.add(pair.first_);
						frame.add(pair.second);
						return then_.apply(frame);
					} else
						return else_.apply(frame);
				};
			}).match(Suite.pattern("DEF-VARS (.0 .1,) .2"), m -> {
				var lazy1 = put(m[0]);
				var value_ = lazy1.lazy_(m[1]);
				var expr = lazy1.lazy_(m[2]);

				return frame -> {
					var value = Mutable.<Thunk_> nil();
					frame.add(() -> value.get().get());
					value.set(() -> value_.apply(frame).get());
					return expr.apply(frame);
				};
			}).match(Matcher.defvars, DEFVARS -> {
				var tuple = Suite.pattern(".0 .1");
				var arrays = Tree.iter(DEFVARS.list).map(tuple::match);
				var size = arrays.size();
				var lazy0 = this;

				for (var array : arrays)
					lazy0 = lazy0.put(array[0]);

				var lazy1 = lazy0;
				var values_ = Read.from(arrays).map(array -> lazy1.lazy_(array[1])).toList();
				var expr = lazy1.lazy_(DEFVARS.do_);

				return frame -> {
					var values = new ArrayList<Thunk_>(size);
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
				var vm1 = IMap.<Node, Fun<Frame, Thunk_>> empty();
				for (var e : vm) {
					var getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}
				var value_ = new Lazy_(0, vm1).put(FUN.param).lazy_(FUN.do_);
				return frame -> () -> new Fun_(in -> {
					var frame1 = new Frame(frame);
					frame1.add(in);
					return value_.apply(frame1);
				});
			}).match(Matcher.if_, IF -> {
				return lazy_(Suite.substitute("APPLY .2 APPLY .1 APPLY .0 VAR if", IF.if_, IF.then_, IF.else_));
			}).match(Matcher.nil, NIL -> {
				return immediate(Atom.NIL);
			}).match(Matcher.number, NUMBER -> {
				return immediate(NUMBER.value);
			}).match(Matcher.pragma, PRAGMA -> {
				return lazy_(PRAGMA.do_);
			}).match(Matcher.tco, TCO -> {
				var iter_ = lazy_(TCO.iter);
				var in_ = lazy_(TCO.in_);
				return frame -> {
					var iter = fun(iter_.apply(frame).get());
					var in = in_.apply(frame);
					Pair_ p0, p1;
					do {
						var out = iter.apply(in);
						p0 = (Pair_) out.get();
						p1 = (Pair_) p0.second.get();
						in = p1.first_;
					} while (p0.first_.get() != Atom.TRUE);
					return p1.second;
				};
			}).match(Matcher.tree, TREE -> {
				return lazy_(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", TREE.op, TREE.left, TREE.right));
			}).match(Matcher.unwrap, UNWRAP -> {
				return lazy_(UNWRAP.do_);
			}).match(Matcher.var, VAR -> {
				return vm.get(VAR.name);
			}).match(Matcher.wrap, WRAP -> {
				return lazy_(WRAP.do_);
			}).nonNullResult();
		}

		private Lazy_ put(Node node) {
			return new Lazy_(fs + 1, vm.put(node, getter(fs)));
		}
	}

	private Fun<Frame, Thunk_> getter(int p) {
		return frame -> frame.get(p);
	}

	private Iterate<Thunk_> fun(Node n) {
		return ((Fun_) n).fun;
	}

	private Fun<Frame, Thunk_> immediate(Node n) {
		return frame -> () -> n;
	}

	private int compare(Node n0, Node n1) {
		var t0 = n0 instanceof Fun_ ? 2 : (n0 instanceof Pair_ ? 1 : 0);
		var t1 = n1 instanceof Fun_ ? 2 : (n0 instanceof Pair_ ? 1 : 0);
		var c = t0 - t1;
		if (c == 0)
			switch (t0) {
			case 0:
				c = Comparer.comparer.compare(n0, n1);
				break;
			case 1:
				var p0 = (Pair_) n0;
				var p1 = (Pair_) n1;
				c = c == 0 ? compare(p0.first_.get(), p1.first_.get()) : c;
				c = c == 0 ? compare(p0.second.get(), p1.second.get()) : c;
				break;
			case 2:
				c = System.identityHashCode(t0) - System.identityHashCode(t1);
			}
		return c;
	}

	private Thunk_ bi(BiFun<Thunk_, Node> fun) {
		return () -> new Fun_(a -> () -> new Fun_(b -> () -> fun.apply(a, b)));
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Thunk_ thunk) {
		return ((Int) thunk.get()).number;
	}

}
