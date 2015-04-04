package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import suite.Suite;
import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;

public class LazyFunInterpreter1 {

	private Atom ERROR = Atom.of("error");
	private Atom FST__ = Atom.of("fst");
	private Atom SND__ = Atom.of("snd");

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
		private Thunk_ first;
		private Thunk_ second;

		private Pair_(Thunk_ left, Thunk_ right) {
			this.first = left;
			this.second = right;
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

		private BiConsumer<Frame, Thunk_> setter(Node v) {
			return (frame, t) -> frame.values.add(t);
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

		private Frame frame(Frame parent) {
			return new Frame(parent);
		}
	}

	public Thunk_ lazy(Node node) {
		Thunk_ error = () -> {
			throw new RuntimeException("Error termination");
		};

		Map<String, Thunk_> df = new HashMap<>();
		df.put(Atom.TRUE.name, () -> Atom.TRUE);
		df.put(Atom.FALSE.name, () -> Atom.FALSE);

		df.put(TermOp.AND___.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> new Pair_(a, b))));
		df.put(TermOp.EQUAL_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) == i(b)))));
		df.put(TermOp.NOTEQ_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) != i(b)))));
		df.put(TermOp.LE____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) <= i(b)))));
		df.put(TermOp.LT____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) < i(b)))));
		df.put(TermOp.GE____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) >= i(b)))));
		df.put(TermOp.GT____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) > i(b)))));
		df.put(TermOp.PLUS__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) + i(b)))));
		df.put(TermOp.MINUS_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) - i(b)))));
		df.put(TermOp.MULT__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) * i(b)))));
		df.put(TermOp.DIVIDE.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) / i(b)))));

		df.put(ERROR.name, error);
		df.put(FST__.name, () -> new Fun_(in -> ((Pair_) in.get()).first));
		df.put(SND__.name, () -> new Fun_(in -> ((Pair_) in.get()).second));

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());

		Mapping mapping = new Mapping(null);
		Frame frame = mapping.frame(null);

		for (String k : keys) {
			Atom var = Atom.of(k);
			mapping = mapping.extend(var);
			mapping.setter(var).accept(frame, df.get(k));
		}

		return lazy0(mapping, node).apply(frame);
	}

	private Fun<Frame, Thunk_> lazy0(Mapping mapping, Node node) {
		Fun<Frame, Thunk_> result;
		Tree tree;
		Node m[];

		if ((m = Suite.matcher(".0 {.1}").apply(node)) != null) {
			Fun<Frame, Thunk_> fun = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> param = lazy0(mapping, m[1]);
			result = frame -> ((Fun_) fun.apply(frame).get()).fun.apply(param.apply(frame));
		} else if ((m = Suite.matcher(".0 := .1 >> .2").apply(node)) != null) {
			Mapping mapping1 = mapping.extend(m[0]);
			BiConsumer<Frame, Thunk_> setter = mapping1.setter(m[0]);
			Fun<Frame, Thunk_> value = lazy0(mapping1, m[1]);
			Fun<Frame, Thunk_> expr = lazy0(mapping1, m[2]);
			result = frame -> {
				Thunk_ val[] = new Thunk_[] { null };
				setter.accept(frame, () -> val[0].get());
				val[0] = value.apply(frame)::get;
				return expr.apply(frame);
			};
		} else if ((m = Suite.matcher(".0 => .1").apply(node)) != null) {
			Mapping mapping1 = new Mapping(mapping).extend(m[0]);
			BiConsumer<Frame, Thunk_> setter = mapping1.setter(m[0]);
			Fun<Frame, Thunk_> value = lazy0(mapping1, m[1]);
			result = frame -> () -> new Fun_(in -> {
				Frame frame1 = mapping1.frame(frame);
				setter.accept(frame1, in);
				return value.apply(frame1);
			});
		} else if ((m = Suite.matcher("if .0 then .1 else .2").apply(node)) != null) {
			Fun<Frame, Thunk_> if_ = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> then_ = lazy0(mapping, m[1]);
			Fun<Frame, Thunk_> else_ = lazy0(mapping, m[2]);
			result = frame -> (if_.apply(frame).get() == Atom.TRUE ? then_ : else_).apply(frame);
		} else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			Fun<Frame, Thunk_> getter = mapping.getter(Atom.of(operator.getName()));
			Fun<Frame, Thunk_> p0 = lazy0(mapping, tree.getLeft());
			Fun<Frame, Thunk_> p1 = lazy0(mapping, tree.getRight());
			result = frame -> {
				Thunk_ r0 = getter.apply(frame);
				Thunk_ r1 = ((Fun_) r0.get()).fun.apply(p0.apply(frame));
				Thunk_ r2 = ((Fun_) r1.get()).fun.apply(p1.apply(frame));
				return r2;
			};
		} else if (node instanceof Atom)
			result = mapping.getter(node);
		else
			result = frame -> () -> node;

		return result;
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Thunk_ thunk) {
		return ((Int) thunk.get()).number;
	}

}
