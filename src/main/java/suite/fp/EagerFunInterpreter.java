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
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class EagerFunInterpreter {

	private boolean isLazyify = false;

	private static class Fun_ extends Node {
		private Fun<Node, Node> fun;

		private Fun_(Fun<Node, Node> fun) {
			this.fun = fun;
		}
	}

	private static class Pair_ extends Node {
		private Node first_;
		private Node second;

		private Pair_(Node first_, Node second) {
			this.first_ = first_;
			this.second = second;
		}
	}

	private static class Wrap_ extends Node {
		private Source<Node> source;

		private Wrap_(Source<Node> source) {
			this.source = source;
		}
	}

	private static class Frame {
		private Frame parent;
		private List<Node> values = new ArrayList<>();

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

		private Fun<Frame, Node> getter(Node var) {
			Integer index = indices.get(var);
			if (index != null) {
				int i = index;
				return frame -> frame.values.get(i);
			} else if (parent != null) {
				Fun<Frame, Node> fun0 = parent.getter(var);
				return frame -> fun0.apply(frame.parent);
			} else
				throw new RuntimeException(var + " not found");
		}

		private BiConsumer<Frame, Node> setter(Node var) {
			return (frame, value) -> frame.values.add(value);
		}

		private Frame frame(Frame parent) {
			return new Frame(parent);
		}
	}

	public Node eager(Node node) {
		Prover prover = new Prover(Suite.createRuleSet(Arrays.asList("auto.sl", "fc/fc.sl")));
		String query = "fc-process-function .0 .1 .2";
		Reference parsed = new Reference();

		if (!prover.prove(Suite.substitute(query, isLazyify ? Atom.of("LAZY") : Atom.of("EAGER"), node, parsed)))
			throw new RuntimeException("Cannot parse " + node);

		Map<String, Node> df = new HashMap<>();
		df.put(Atom.TRUE.name, Atom.TRUE);
		df.put(Atom.FALSE.name, Atom.FALSE);

		df.put(TermOp.AND___.getName(), binary((a, b) -> new Pair_(a, b)));
		df.put(TermOp.EQUAL_.getName(), binary((a, b) -> b(compare(a, b) == 0)));
		df.put(TermOp.NOTEQ_.getName(), binary((a, b) -> b(compare(a, b) != 0)));
		df.put(TermOp.LE____.getName(), binary((a, b) -> b(compare(a, b) <= 0)));
		df.put(TermOp.LT____.getName(), binary((a, b) -> b(compare(a, b) < 0)));
		df.put(TermOp.GE____.getName(), binary((a, b) -> b(compare(a, b) >= 0)));
		df.put(TermOp.GT____.getName(), binary((a, b) -> b(compare(a, b) > 0)));
		df.put(TermOp.PLUS__.getName(), binary((a, b) -> Int.of(i(a) + i(b))));
		df.put(TermOp.MINUS_.getName(), binary((a, b) -> Int.of(i(a) - i(b))));
		df.put(TermOp.MULT__.getName(), binary((a, b) -> Int.of(i(a) * i(b))));
		df.put(TermOp.DIVIDE.getName(), binary((a, b) -> Int.of(i(a) / i(b))));

		df.put("fst", new Fun_(in -> ((Pair_) in).first_));
		df.put("if", new Fun_(a -> new Fun_(b -> new Fun_(c -> a == Atom.TRUE ? b : c))));
		df.put("snd", new Fun_(in -> ((Pair_) in).second));

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());
		Mapping mapping = new Mapping(null);
		Frame frame = mapping.frame(null);

		for (String key : keys) {
			Atom var = Atom.of(key);
			mapping = mapping.extend(var);
			mapping.setter(var).accept(frame, df.get(key));
		}

		return eager0(mapping, parsed).apply(frame);
	}

	public void setLazyify(boolean isLazyify) {
		this.isLazyify = isLazyify;
	}

	private Fun<Frame, Node> eager0(Mapping mapping, Node node) {
		Fun<Frame, Node> result;
		Node m[];

		if ((m = Suite.matcher("ATOM .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("BOOLEAN .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("DEF-VARS .0 .1").apply(node)) != null) {
			Streamlet<Node[]> arrays = Tree.iter(m[0]).map(Suite.matcher(".0 .1")::apply);
			Mapping mapping1 = arrays //
					.map(m1 -> m1[0]) //
					.fold(mapping, Mapping::extend);
			List<Pair<BiConsumer<Frame, Node>, Fun<Frame, Node>>> svs = arrays //
					.map(m1 -> Pair.of(mapping1.setter(m1[0]), eager0(mapping1, m1[1]))) //
					.toList();
			Fun<Frame, Node> expr = eager0(mapping1, m[1]);

			result = frame -> {
				for (Pair<BiConsumer<Frame, Node>, Fun<Frame, Node>> sv : svs)
					sv.t0.accept(frame, sv.t1.apply(frame));
				return expr.apply(frame);
			};
		} else if ((m = Suite.matcher("ERROR").apply(node)) != null)
			result = frame -> {
				throw new RuntimeException("Error termination");
			};
		else if ((m = Suite.matcher("FUN .0 .1").apply(node)) != null) {
			Mapping mapping1 = new Mapping(mapping).extend(m[0]);
			BiConsumer<Frame, Node> setter = mapping1.setter(m[0]);
			Fun<Frame, Node> value_ = eager0(mapping1, m[1]);
			result = frame -> new Fun_(in -> {
				Frame frame1 = mapping1.frame(frame);
				setter.accept(frame1, in);
				return value_.apply(frame1);
			});
		} else if ((m = Suite.matcher("IF .0 .1 .2").apply(node)) != null) {
			Fun<Frame, Node> if_ = eager0(mapping, m[0]);
			Fun<Frame, Node> then_ = eager0(mapping, m[1]);
			Fun<Frame, Node> else_ = eager0(mapping, m[2]);
			result = frame -> (if_.apply(frame) == Atom.TRUE ? then_ : else_).apply(frame);
		} else if ((m = Suite.matcher("INVOKE .0 .1").apply(node)) != null) {
			Fun<Frame, Node> param_ = eager0(mapping, m[0]);
			Fun<Frame, Node> fun_ = eager0(mapping, m[1]);
			result = frame -> {
				Node fun = fun_.apply(frame);
				Node param = param_.apply(frame);
				return ((Fun_) fun).fun.apply(param);
			};
		} else if ((m = Suite.matcher("NEW-VAR .0").apply(node)) != null)
			result = mapping.getter(m[0]);
		else if ((m = Suite.matcher("NUMBER .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("PAIR .0 .1").apply(node)) != null) {
			Fun<Frame, Node> left_ = eager0(mapping, m[0]);
			Fun<Frame, Node> right_ = eager0(mapping, m[1]);
			result = frame -> new Pair_(left_.apply(frame), right_.apply(frame));
		} else if ((m = Suite.matcher("PRAGMA .0 .1").apply(node)) != null)
			result = eager0(mapping, m[1]);
		else if ((m = Suite.matcher("TCO .0 .1").apply(node)) != null) {
			Fun<Frame, Node> iter_ = eager0(mapping, m[0]);
			Fun<Frame, Node> in_ = eager0(mapping, m[1]);
			result = frame -> {
				Fun_ iter = (Fun_) iter_.apply(frame);
				Node in = in_.apply(frame);
				Pair_ p0, p1;
				do {
					Node out = iter.fun.apply(in);
					p0 = (Pair_) out;
					p1 = (Pair_) p0.second;
					in = p1.first_;
				} while (p0.first_ != Atom.TRUE);
				return p1.second;
			};
		} else if ((m = Suite.matcher("TREE .0 .1 .2").apply(node)) != null)
			result = eager0(mapping, Suite.substitute("INVOKE .2 INVOKE .1 (VAR .0)", m[0], m[1], m[2]));
		else if ((m = Suite.matcher("UNWRAP .0").apply(node)) != null) {
			Fun<Frame, Node> value_ = eager0(mapping, m[0]);
			result = frame -> ((Wrap_) value_.apply(frame)).source.source();
		} else if ((m = Suite.matcher("VAR .0").apply(node)) != null)
			result = mapping.getter(m[0]);
		else if ((m = Suite.matcher("WRAP .0").apply(node)) != null) {
			Fun<Frame, Node> value_ = eager0(mapping, m[0]);
			result = frame -> new Wrap_(() -> value_.apply(frame));
		} else
			throw new RuntimeException("Unrecognized construct " + node);

		return result;
	}

	private Fun<Frame, Node> immediate(Node n) {
		return frame -> n;
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
				c = c == 0 ? compare(p0.first_, p1.first_) : c;
				c = c == 0 ? compare(p0.second, p1.second) : c;
				break;
			case 2:
				c = System.identityHashCode(t0) - System.identityHashCode(t1);
			}
		return c;
	}

	private Node binary(BiFunction<Node, Node, Node> fun) {
		return new Fun_(a -> new Fun_(b -> fun.apply(a, b)));
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Node thunk) {
		return ((Int) thunk).number;
	}

}
