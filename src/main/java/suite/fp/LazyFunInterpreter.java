package suite.fp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import suite.Suite;
import suite.adt.Pair;
import suite.immutable.IMap;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.Mutable;
import suite.node.util.TreeUtil;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

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
		df.put(TermOp.GE____.getName(), binary((a, b) -> b(compare(a.get(), b.get()) >= 0)));
		df.put(TermOp.GT____.getName(), binary((a, b) -> b(compare(a.get(), b.get()) > 0)));
		df.put(TermOp.PLUS__.getName(), binary((a, b) -> Int.of(i(a) + i(b))));
		df.put(TermOp.MINUS_.getName(), binary((a, b) -> Int.of(i(a) - i(b))));
		df.put(TermOp.MULT__.getName(), binary((a, b) -> Int.of(i(a) * i(b))));
		df.put(TermOp.DIVIDE.getName(), binary((a, b) -> Int.of(i(a) / i(b))));

		df.put("fst", () -> new Fun_(in -> ((Pair_) in.get()).first_));
		df.put("if", () -> new Fun_(a -> () -> new Fun_(b -> () -> new Fun_(c -> a.get() == Atom.TRUE ? b : c))));
		df.put("snd", () -> new Fun_(in -> ((Pair_) in.get()).second));

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());
		IMap<Node, Fun<Frame, Thunk_>> vm = IMap.empty();
		int fs = 0;
		Frame frame = new Frame(null);

		for (String key : keys) {
			vm = vm.put(Atom.of(key), getter(fs++));
			frame.add(df.get(key));
		}

		return lazy0(fs, vm, parsed).apply(frame);
	}

	private Reference parse(Node node) {
		Prover prover = new Prover(Suite.createRuleSet(Arrays.asList("auto.sl", "fc/fc.sl")));

		Reference parsed = new Reference();
		if (!prover.prove(Suite.substitute("fc-parse .0 .1", node, parsed)))
			throw new RuntimeException("Cannot parse " + node);
		return parsed;
	}

	private Fun<Frame, Thunk_> lazy0(int fs, IMap<Node, Fun<Frame, Thunk_>> vm, Node node) {
		Fun<Frame, Thunk_> result;
		Node m[];

		if ((m = Suite.matcher("APPLY .0 .1").apply(node)) != null) {
			Fun<Frame, Thunk_> param_ = lazy0(fs, vm, m[0]);
			Fun<Frame, Thunk_> fun_ = lazy0(fs, vm, m[1]);
			result = frame -> {
				Thunk_ fun = fun_.apply(frame);
				Thunk_ param = param_.apply(frame);
				return () -> fun(fun.get()).apply(param).get();
			};
		} else if ((m = Suite.matcher("ATOM .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("BOOLEAN .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("CONS _ .0 .1").apply(node)) != null) {
			Fun<Frame, Thunk_> p0_ = lazy0(fs, vm, m[0]);
			Fun<Frame, Thunk_> p1_ = lazy0(fs, vm, m[1]);
			result = frame -> () -> new Pair_(p0_.apply(frame), p1_.apply(frame));
		} else if ((m = Suite.matcher("DECONS .0 .1 .2 .3 .4 .5").apply(node)) != null) {
			Fun<Frame, Thunk_> value_ = lazy0(fs, vm, m[1]);
			IMap<Node, Fun<Frame, Thunk_>> vm1 = vm.put(m[2], getter(fs)).put(m[3], getter(fs + 1));
			Fun<Frame, Thunk_> then_ = lazy0(fs + 2, vm1, m[4]);
			Fun<Frame, Thunk_> else_ = lazy0(fs, vm, m[5]);

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
		} else if ((m = Suite.matcher("DEF-VARS (.0 .1,) .2").apply(node)) != null) {
			IMap<Node, Fun<Frame, Thunk_>> vm1 = vm.put(m[0], getter(fs));
			int fs1 = fs + 1;
			Fun<Frame, Thunk_> value_ = lazy0(fs1, vm1, m[1]);
			Fun<Frame, Thunk_> expr = lazy0(fs1, vm1, m[2]);

			result = frame -> {
				Mutable<Thunk_> value = Mutable.nil();
				frame.add(() -> value.get().get());
				value.set(value_.apply(frame)::get);
				return expr.apply(frame);
			};
		} else if ((m = Suite.matcher("DEF-VARS .0 .1").apply(node)) != null) {
			Streamlet<Node[]> arrays = Tree.iter(m[0]).map(TreeUtil::tuple);
			int size = arrays.size();
			IMap<Node, Fun<Frame, Thunk_>> vm1 = vm;
			int fs1 = fs;

			for (Node array[] : arrays)
				vm1 = vm1.put(array[0], getter(fs1++));
			List<Fun<Frame, Thunk_>> values_ = new ArrayList<>();
			for (Node array[] : arrays)
				values_.add(lazy0(fs1, vm1, array[1]));
			Fun<Frame, Thunk_> expr = lazy0(fs1, vm1, m[1]);

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
		} else if ((m = Suite.matcher("ERROR").apply(node)) != null)
			result = frame -> () -> {
				throw new RuntimeException("Error termination");
			};
		else if ((m = Suite.matcher("FUN .0 .1").apply(node)) != null) {
			IMap<Node, Fun<Frame, Thunk_>> vm1 = IMap.empty();
			for (Pair<Node, Fun<Frame, Thunk_>> pair : vm) {
				Fun<Frame, Thunk_> getter0 = pair.t1;
				vm1 = vm1.put(pair.t0, frame -> getter0.apply(frame.parent));
			}
			vm1 = vm1.put(m[0], getter(0));

			Fun<Frame, Thunk_> value_ = lazy0(1, vm1, m[1]);
			result = frame -> () -> new Fun_(in -> {
				Frame frame1 = new Frame(frame);
				frame1.add(in);
				return value_.apply(frame1);
			});
		} else if ((m = Suite.matcher("IF .0 .1 .2").apply(node)) != null)
			result = lazy0(fs, vm, Suite.substitute("APPLY .2 APPLY .1 APPLY .0 VAR if", m[0], m[1], m[2]));
		else if ((m = Suite.matcher("NIL").apply(node)) != null)
			result = immediate(Atom.NIL);
		else if ((m = Suite.matcher("NUMBER .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("PRAGMA .0 .1").apply(node)) != null)
			result = lazy0(fs, vm, m[1]);
		else if ((m = Suite.matcher("TCO .0 .1").apply(node)) != null) {
			Fun<Frame, Thunk_> iter_ = lazy0(fs, vm, m[0]);
			Fun<Frame, Thunk_> in_ = lazy0(fs, vm, m[1]);
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
		} else if ((m = Suite.matcher("TREE .0 .1 .2").apply(node)) != null)
			result = lazy0(fs, vm, Suite.substitute("APPLY .2 APPLY .1 (VAR .0)", m[0], m[1], m[2]));
		else if ((m = Suite.matcher("UNWRAP .0").apply(node)) != null)
			return lazy0(fs, vm, m[0]);
		else if ((m = Suite.matcher("VAR .0").apply(node)) != null)
			result = vm.get(m[0]);
		else if ((m = Suite.matcher("WRAP .0").apply(node)) != null)
			return lazy0(fs, vm, m[0]);
		else
			throw new RuntimeException("Unrecognized construct " + node);

		return result;
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
