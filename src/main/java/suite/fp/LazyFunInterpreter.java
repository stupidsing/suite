package suite.fp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.adt.pair.Pair;
import suite.fp.match.Matcher;
import suite.fp.match.Matchers.APPLY;
import suite.fp.match.Matchers.ATOM;
import suite.fp.match.Matchers.BOOLEAN;
import suite.fp.match.Matchers.CHARS;
import suite.fp.match.Matchers.CONS;
import suite.fp.match.Matchers.DECONS;
import suite.fp.match.Matchers.DEFVARS;
import suite.fp.match.Matchers.ERROR;
import suite.fp.match.Matchers.FUN;
import suite.fp.match.Matchers.IF;
import suite.fp.match.Matchers.NUMBER;
import suite.fp.match.Matchers.PRAGMA;
import suite.fp.match.Matchers.TCO;
import suite.fp.match.Matchers.TREE;
import suite.fp.match.Matchers.UNWRAP;
import suite.fp.match.Matchers.VAR;
import suite.fp.match.Matchers.WRAP;
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
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.Mutable;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class LazyFunInterpreter {

	public interface Thunk_ {
		public Node get();
	}

	private static class Fun_ extends Node {
		private Fun<Thunk_, Thunk_> fun;

		private Fun_(Fun<Thunk_, Thunk_> fun) {
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
		Node parsed = parse(node);

		Map<String, Thunk_> df = new HashMap<>();
		df.put(TermOp.AND___.getName(), binary((a, b) -> new Pair_(a, b)));
		df.put(TermOp.EQUAL_.getName(), binary((a, b) -> b(compare(a.get(), b.get()) == 0)));
		df.put(TermOp.NOTEQ_.getName(), binary((a, b) -> b(compare(a.get(), b.get()) != 0)));
		df.put(TermOp.LE____.getName(), binary((a, b) -> b(compare(a.get(), b.get()) <= 0)));
		df.put(TermOp.LT____.getName(), binary((a, b) -> b(compare(a.get(), b.get()) < 0)));
		df.put(TermOp.PLUS__.getName(), binary((a, b) -> Int.of(i(a) + i(b))));
		df.put(TermOp.MINUS_.getName(), binary((a, b) -> Int.of(i(a) - i(b))));
		df.put(TermOp.MULT__.getName(), binary((a, b) -> Int.of(i(a) * i(b))));
		df.put(TermOp.DIVIDE.getName(), binary((a, b) -> Int.of(i(a) / i(b))));

		df.put("fst", () -> new Fun_(in -> ((Pair_) in.get()).first_));
		df.put("if", () -> new Fun_(a -> () -> new Fun_(b -> () -> new Fun_(c -> a.get() == Atom.TRUE ? b : c))));
		df.put("snd", () -> new Fun_(in -> ((Pair_) in.get()).second));

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());
		Lazy_ lazy0 = new Lazy_(0, IMap.empty());
		Frame frame = new Frame(null);

		for (String key : keys) {
			lazy0 = lazy0.put(Atom.of(key));
			frame.add(df.get(key));
		}

		return lazy0.lazy_(parsed).apply(frame);
	}

	private Reference parse(Node node) {
		Prover prover = new Prover(Suite.newRuleSet(Arrays.asList("auto.sl", "fc/fc.sl")));

		Reference parsed = new Reference();
		if (!prover.prove(Suite.substitute("fc-parse .0 .1", node, parsed)))
			throw new RuntimeException("cannot parse " + node);
		return parsed;
	}

	private class Lazy_ {
		private int fs;
		private IMap<Node, Fun<Frame, Thunk_>> vm;

		private Lazy_(int fs, IMap<Node, Fun<Frame, Thunk_>> vm) {
			this.fs = fs;
			this.vm = vm;
		}

		private Fun<Frame, Thunk_> lazy_(Node node) {
			Fun<Frame, Thunk_> result;
			Node[] m;
			APPLY APPLY;
			ATOM ATOM;
			BOOLEAN BOOLEAN;
			CHARS CHARS;
			CONS CONS;
			DECONS DECONS;
			DEFVARS DEFVARS;
			ERROR ERROR;
			FUN FUN;
			IF IF;
			NUMBER NUMBER;
			PRAGMA PRAGMA;
			TCO TCO;
			TREE TREE;
			UNWRAP UNWRAP;
			VAR VAR;
			WRAP WRAP;

			if ((APPLY = Matcher.apply.match(node)) != null) {
				Fun<Frame, Thunk_> param_ = lazy_(APPLY.param);
				Fun<Frame, Thunk_> fun_ = lazy_(APPLY.fun);
				result = frame -> {
					Thunk_ fun = fun_.apply(frame);
					Thunk_ param = param_.apply(frame);
					return () -> fun(fun.get()).apply(param).get();
				};
			} else if ((ATOM = Matcher.atom.match(node)) != null)
				result = immediate(ATOM.value);
			else if ((BOOLEAN = Matcher.boolean_.match(node)) != null)
				result = immediate(BOOLEAN.value);
			else if ((CHARS = Matcher.chars.match(node)) != null)
				result = immediate(new Data<>(To.chars(((Str) CHARS.value).value)));
			else if ((CONS = Matcher.cons.match(node)) != null) {
				Fun<Frame, Thunk_> p0_ = lazy_(CONS.head);
				Fun<Frame, Thunk_> p1_ = lazy_(CONS.tail);
				result = frame -> () -> new Pair_(p0_.apply(frame), p1_.apply(frame));
			} else if ((DECONS = Matcher.decons.match(node)) != null) {
				Fun<Frame, Thunk_> value_ = lazy_(DECONS.value);
				Fun<Frame, Thunk_> then_ = put(DECONS.left).put(DECONS.right).lazy_(DECONS.then);
				Fun<Frame, Thunk_> else_ = lazy_(DECONS.else_);

				result = frame -> {
					Node value = value_.apply(frame).get();
					if (value instanceof Pair_) {
						Pair_ pair = (Pair_) value;
						frame.add(pair.first_);
						frame.add(pair.second);
						return then_.apply(frame);
					} else
						return else_.apply(frame);
				};
			} else if ((m = Suite.match("DEF-VARS (.0 .1,) .2").apply(node)) != null) {
				Lazy_ lazy1 = put(m[0]);
				Fun<Frame, Thunk_> value_ = lazy1.lazy_(m[1]);
				Fun<Frame, Thunk_> expr = lazy1.lazy_(m[2]);

				result = frame -> {
					Mutable<Thunk_> value = Mutable.nil();
					frame.add(() -> value.get().get());
					value.set(() -> value_.apply(frame).get());
					return expr.apply(frame);
				};
			} else if ((DEFVARS = Matcher.defvars.match(node)) != null) {
				Match tuple = Suite.match(".0 .1");
				Streamlet<Node[]> arrays = Tree.iter(DEFVARS.list).map(tuple::apply);
				int size = arrays.size();
				Lazy_ lazy0 = this;

				for (Node[] array : arrays)
					lazy0 = lazy0.put(array[0]);

				List<Fun<Frame, Thunk_>> values_ = new ArrayList<>();
				for (Node[] array : arrays)
					values_.add(lazy0.lazy_(array[1]));

				Fun<Frame, Thunk_> expr = lazy0.lazy_(DEFVARS.do_);

				result = frame -> {
					List<Thunk_> values = new ArrayList<>(size);
					for (int i = 0; i < size; i++) {
						int i1 = i;
						frame.add(() -> values.get(i1).get());
					}
					for (Fun<Frame, Thunk_> value_ : values_)
						values.add(value_.apply(frame)::get);
					return expr.apply(frame);
				};
			} else if ((ERROR = Matcher.error.match(node)) != null)
				result = frame -> () -> {
					throw new RuntimeException("error termination " + Formatter.display(ERROR.m));
				};
			else if ((FUN = Matcher.fun.match(node)) != null) {
				IMap<Node, Fun<Frame, Thunk_>> vm1 = IMap.empty();
				for (Pair<Node, Fun<Frame, Thunk_>> e : vm) {
					Fun<Frame, Thunk_> getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}

				Fun<Frame, Thunk_> value_ = new Lazy_(0, vm1).put(FUN.param).lazy_(FUN.do_);
				result = frame -> () -> new Fun_(in -> {
					Frame frame1 = new Frame(frame);
					frame1.add(in);
					return value_.apply(frame1);
				});
			} else if ((IF = Matcher.if_.match(node)) != null)
				result = lazy_(Suite.substitute("APPLY .2 APPLY .1 APPLY .0 VAR if", IF.if_, IF.then_, IF.else_));
			else if (Matcher.nil.match(node) != null)
				result = immediate(Atom.NIL);
			else if ((NUMBER = Matcher.number.match(node)) != null)
				result = immediate(NUMBER.value);
			else if ((PRAGMA = Matcher.pragma.match(node)) != null)
				result = lazy_(PRAGMA.do_);
			else if ((TCO = Matcher.tco.match(node)) != null) {
				Fun<Frame, Thunk_> iter_ = lazy_(TCO.iter);
				Fun<Frame, Thunk_> in_ = lazy_(TCO.in_);
				result = frame -> {
					Fun<Thunk_, Thunk_> iter = fun(iter_.apply(frame).get());
					Thunk_ in = in_.apply(frame);
					Pair_ p0, p1;
					do {
						Thunk_ out = iter.apply(in);
						p0 = (Pair_) out.get();
						p1 = (Pair_) p0.second.get();
						in = p1.first_;
					} while (p0.first_.get() != Atom.TRUE);
					return p1.second;
				};
			} else if ((TREE = Matcher.tree.match(node)) != null)
				result = lazy_(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", TREE.op, TREE.left, TREE.right));
			else if ((UNWRAP = Matcher.unwrap.match(node)) != null)
				result = lazy_(UNWRAP.do_);
			else if ((VAR = Matcher.var.match(node)) != null)
				result = vm.get(VAR.name);
			else if ((WRAP = Matcher.wrap.match(node)) != null)
				result = lazy_(WRAP.do_);
			else
				throw new RuntimeException("unrecognized construct " + node);

			return result;
		}

		private Lazy_ put(Node node) {
			return new Lazy_(fs + 1, vm.put(node, getter(fs)));
		}
	}

	private Fun<Frame, Thunk_> getter(int p) {
		return frame -> frame.get(p);
	}

	private Fun<Thunk_, Thunk_> fun(Node n) {
		return ((Fun_) n).fun;
	}

	private Fun<Frame, Thunk_> immediate(Node n) {
		return frame -> () -> n;
	}

	private int compare(Node n0, Node n1) {
		int t0 = n0 instanceof Fun_ ? 2 : (n0 instanceof Pair_ ? 1 : 0);
		int t1 = n1 instanceof Fun_ ? 2 : (n0 instanceof Pair_ ? 1 : 0);
		int c = t0 - t1;
		if (c == 0)
			switch (t0) {
			case 0:
				c = Comparer.comparer.compare(n0, n1);
				break;
			case 1:
				Pair_ p0 = (Pair_) n0;
				Pair_ p1 = (Pair_) n1;
				c = c == 0 ? compare(p0.first_.get(), p1.first_.get()) : c;
				c = c == 0 ? compare(p0.second.get(), p1.second.get()) : c;
				break;
			case 2:
				c = System.identityHashCode(t0) - System.identityHashCode(t1);
			}
		return c;
	}

	private Thunk_ binary(BiFunction<Thunk_, Thunk_, Node> fun) {
		return () -> new Fun_(a -> () -> new Fun_(b -> () -> fun.apply(a, b)));
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Thunk_ thunk) {
		return ((Int) thunk.get()).number;
	}

}
