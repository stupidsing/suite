package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import suite.Suite;
import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class LazyFunInterpreter1 {

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

		df.put("error", error);
		df.put("fst", () -> new Fun_(in -> ((Pair_) in.get()).first_));
		df.put("snd", () -> new Fun_(in -> ((Pair_) in.get()).second));

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());

		Mapping mapping = new Mapping(null);
		Frame frame = mapping.frame(null);

		for (String key : keys) {
			Atom var = Atom.of(key);
			mapping = mapping.extend(var);
			mapping.setter(var).accept(frame, df.get(key));
		}

		return lazy0(mapping, node).apply(frame);
	}

	private Fun<Frame, Thunk_> lazy0(Mapping mapping, Node node) {
		Fun<Frame, Thunk_> result;
		Tree tree;
		Node m[];

		if ((m = Suite.matcher(".0 := .1 >> .2").apply(node)) != null)
			result = lazy0(mapping, Suite.substitute("(.0 := .1), >> .2", m));
		else if ((m = Suite.matcher(".0 >> .1").apply(node)) != null) {
			List<Node[]> arrays = Read.from(Tree.iter(m[0])).map(Suite.matcher(".0 := .1")::apply).toList();
			List<Node> vars = Read.from(arrays).map(m1 -> m1[0]).toList();
			int size = vars.size();

			Mapping mapping1 = Read.from(vars).fold(mapping, Mapping::extend);
			List<BiConsumer<Frame, Thunk_>> setters = Read.from(vars).map(mapping1::setter).toList();
			List<Fun<Frame, Thunk_>> values = Read.from(arrays).map(m1 -> lazy0(mapping1, m1[1])).toList();
			Fun<Frame, Thunk_> expr = lazy0(mapping1, m[1]);

			result = frame -> {
				Thunk_ val[] = new Thunk_[size];
				for (int i = 0; i < size; i++) {
					int i1 = i;
					setters.get(i).accept(frame, () -> val[i1].get());
				}
				for (int i = 0; i < size; i++)
					val[i] = values.get(i).apply(frame)::get;
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
		} else if ((m = Suite.matcher(".0 {.1}").apply(node)) != null) {
			Fun<Frame, Thunk_> fun = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> param = lazy0(mapping, m[1]);
			result = frame -> ((Fun_) fun.apply(frame).get()).fun.apply(param.apply(frame));
		} else if ((m = Suite.matcher("if .0 then .1 else .2").apply(node)) != null) {
			Fun<Frame, Thunk_> if_ = lazy0(mapping, m[0]);
			Fun<Frame, Thunk_> then_ = lazy0(mapping, m[1]);
			Fun<Frame, Thunk_> else_ = lazy0(mapping, m[2]);
			result = frame -> (if_.apply(frame).get() == Atom.TRUE ? then_ : else_).apply(frame);
		} else if ((tree = Tree.decompose(node)) != null)
			return lazy0(mapping, Suite.substitute(".0 {.1} {.2}" //
					, Atom.of(tree.getOperator().getName()) //
					, tree.getLeft() //
					, tree.getRight()));
		else if (node instanceof Atom)
			result = mapping.getter(node);
		else
			result = frame -> () -> node;

		return result;
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
