package suite.fp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import suite.Suite;
import suite.immutable.IMap;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
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

	private static class Frame {
		private Frame parent;
		private List<Thunk_> values = new ArrayList<>();

		private Frame(Frame parent) {
			this.parent = parent;
		}
	}

	private static class Mapping {
		private Mapping parent;
		private int size;
		private IMap<Node, Integer> indices;

		private Mapping(Mapping parent) {
			this(parent, 0, new IMap<>());
		}

		private Mapping(Mapping parent, int size, IMap<Node, Integer> indices) {
			this.parent = parent;
			this.size = size;
			this.indices = indices;
		}

		private Mapping extend(Node v) {
			int index = size;
			return new Mapping(parent, size + 1, indices.put(v, index));
		}

		private Fun<Frame, Thunk_> getter(Node v) {
			Integer index = indices.get(v);
			if (index != null) {
				int i = index;
				return frame -> frame.values.get(i);
			} else if (parent != null) {
				Fun<Frame, Thunk_> fun0 = parent.getter(v);
				return frame -> fun0.apply(frame.parent);
			} else
				throw new RuntimeException(v + " not found");
		}

		private BiConsumer<Frame, Thunk_> setter(Node v) {
			return (frame, t) -> frame.values.add(t);
		}

		private Frame frame(Frame parent) {
			return new Frame(parent);
		}
	}

	public Thunk_ lazy(Node node) {
		Reference parsed = new Reference();

		Prover prover = new Prover(Suite.createRuleSet(Arrays.asList("auto.sl", "fc/fc.sl")));
		if (!prover.prove(Suite.substitute("fc-parse .0 .1", node, parsed)))
			throw new RuntimeException("Cannot parse " + node);

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
		Mapping mapping = new Mapping(null);
		Frame frame = mapping.frame(null);

		for (String key : keys) {
			Atom var = Atom.of(key);
			mapping = mapping.extend(var);
			mapping.setter(var).accept(frame, df.get(key));
		}

		return lazy0(mapping, parsed).apply(frame);
	}

	private Fun<Frame, Thunk_> lazy0(Mapping mapping, Node node) {
		Fun<Frame, Thunk_> result;
		Node m[];

		if ((m = Suite.matcher("ATOM .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("BOOLEAN .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("DEF-VARS .0 .1").apply(node)) != null) {
			Streamlet<Node[]> arrays = Tree.iter(m[0]).map(Suite.matcher(".0 .1")::apply);
			Streamlet<Node> vars = arrays.map(m1 -> m1[0]);
			int size = vars.size();

			Mapping mapping1 = vars.fold(mapping, Mapping::extend);
			List<BiConsumer<Frame, Thunk_>> setters = vars.map(mapping1::setter).toList();
			List<Fun<Frame, Thunk_>> values_ = arrays.map(m1 -> lazy0(mapping1, m1[1])).toList();
			Fun<Frame, Thunk_> expr = lazy0(mapping1, m[1]);

			result = frame -> {
				List<Thunk_> values = new ArrayList<Thunk_>(size);
				for (int i = 0; i < size; i++) {
					int i1 = i;
					setters.get(i).accept(frame, () -> values.get(i1).get());
				}
				for (int i = 0; i < size; i++)
					values.add(values_.get(i).apply(frame)::get);
				return expr.apply(frame);
			};
		} else if ((m = Suite.matcher("ERROR").apply(node)) != null)
			result = frame -> {
				throw new RuntimeException("Error termination");
			};
		else if ((m = Suite.matcher("FUN .0 .1").apply(node)) != null) {
			Mapping mapping1 = new Mapping(mapping).extend(m[0]);
			BiConsumer<Frame, Thunk_> setter = mapping1.setter(m[0]);
			Fun<Frame, Thunk_> value_ = lazy0(mapping1, m[1]);
			result = frame -> () -> new Fun_(in -> {
				Frame frame1 = mapping1.frame(frame);
				setter.accept(frame1, in);
				return value_.apply(frame1);
			});
		} else if ((m = Suite.matcher("IF .0 .1 .2").apply(node)) != null)
			result = lazy0(mapping, Suite.substitute("INVOKE .2 INVOKE .1 INVOKE .0 VAR if", m[0], m[1], m[2]));
		else if ((m = Suite.matcher("INVOKE .0 .1").apply(node)) != null) {
			Fun<Frame, Thunk_> param_ = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> fun_ = lazy0(mapping, m[1]);
			result = frame -> {
				Thunk_ fun = fun_.apply(frame);
				Thunk_ param = param_.apply(frame);
				return () -> ((Fun_) fun.get()).fun.apply(param).get();
			};
		} else if ((m = Suite.matcher("NEW-VAR .0").apply(node)) != null)
			result = mapping.getter(m[0]);
		else if ((m = Suite.matcher("NUMBER .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("PAIR .0 .1").apply(node)) != null) {
			Fun<Frame, Thunk_> left_ = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> right_ = lazy0(mapping, m[1]);
			result = frame -> () -> new Pair_(left_.apply(frame), right_.apply(frame));
		} else if ((m = Suite.matcher("PRAGMA .0 .1").apply(node)) != null)
			result = lazy0(mapping, m[1]);
		else if ((m = Suite.matcher("TCO .0 .1").apply(node)) != null) {
			Fun<Frame, Thunk_> iter_ = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> in_ = lazy0(mapping, m[1]);
			result = frame -> {
				Fun_ iter = (Fun_) iter_.apply(frame).get();
				Thunk_ in = in_.apply(frame);
				Pair_ p0, p1;
				do {
					Thunk_ out = iter.fun.apply(in);
					p0 = (Pair_) out.get();
					p1 = (Pair_) p0.second.get();
					in = p1.first_;
				} while (p0.first_.get() != Atom.TRUE);
				return p1.second;
			};
		} else if ((m = Suite.matcher("TREE .0 .1 .2").apply(node)) != null)
			result = lazy0(mapping, Suite.substitute("INVOKE .2 INVOKE .1 (VAR .0)", m[0], m[1], m[2]));
		else if ((m = Suite.matcher("VAR .0").apply(node)) != null)
			result = mapping.getter(m[0]);
		else
			throw new RuntimeException("Unrecognized construct " + node);

		return result;
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
