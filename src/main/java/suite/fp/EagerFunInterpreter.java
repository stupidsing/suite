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
import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.immutable.IMap;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.TreeUtil;
import suite.primitive.Chars;
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

	private static class Wrap_ extends Node {
		private Source<Node> source;

		private Wrap_(Source<Node> source) {
			this.source = source;
		}
	}

	private static class Frame extends ArrayList<Node> {
		private static final long serialVersionUID = 1l;
		private Frame parent;

		private Frame(Frame parent) {
			this.parent = parent;
		}
	}

	public Node eager(Node node) {
		Node mode = isLazyify ? Atom.of("LAZY") : Atom.of("EAGER");
		Node query = Suite.substitute("source .in, fc-process-function .0 .in .out, sink .out", mode);

		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl", "fc/fc.sl"));
		Finder finder = new SewingProverBuilder2().build(rs).apply(query);
		Node parsed = FindUtil.collectSingle(finder, node);
		IntrinsicCallback ic = isLazyify ? lazyIntrinsicCallback() : Intrinsics.eagerIntrinsicCallback;

		Map<String, Node> df = new HashMap<>();
		df.put(TermOp.AND___.getName(), f2((a, b) -> Tree.of(TermOp.AND___, a, b)));
		df.put(TermOp.EQUAL_.getName(), f2((a, b) -> b(compare(a, b) == 0)));
		df.put(TermOp.NOTEQ_.getName(), f2((a, b) -> b(compare(a, b) != 0)));
		df.put(TermOp.LE____.getName(), f2((a, b) -> b(compare(a, b) <= 0)));
		df.put(TermOp.LT____.getName(), f2((a, b) -> b(compare(a, b) < 0)));
		df.put(TermOp.GE____.getName(), f2((a, b) -> b(compare(a, b) >= 0)));
		df.put(TermOp.GT____.getName(), f2((a, b) -> b(compare(a, b) > 0)));
		df.put(TermOp.PLUS__.getName(), f2((a, b) -> Int.of(i(a) + i(b))));
		df.put(TermOp.MINUS_.getName(), f2((a, b) -> Int.of(i(a) - i(b))));
		df.put(TermOp.MULT__.getName(), f2((a, b) -> Int.of(i(a) * i(b))));
		df.put(TermOp.DIVIDE.getName(), f2((a, b) -> Int.of(i(a) / i(b))));

		df.put("+call%i-t1", f1(i -> fn(1, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-t2", f1(i -> fn(2, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-t3", f1(i -> fn(3, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-v1", f1(i -> fn(1, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-v2", f1(i -> fn(2, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-v3", f1(i -> fn(3, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+compare", f2((a, b) -> Int.of(Comparer.comparer.compare(a, b))));
		df.put("+get%i", f1(a -> new Data<>(Intrinsics.intrinsics.get(((Atom) a).name.split("!")[1]))));
		df.put("+is-list", f1(a -> b(Tree.decompose(a) != null)));
		df.put("+is-pair", f1(a -> b(Tree.decompose(a) != null)));
		df.put("+lcons", f2((a, b) -> Tree.of(TermOp.OR____, a, b)));
		df.put("+lhead", f1(a -> Tree.decompose(a).getLeft()));
		df.put("+ltail", f1(a -> Tree.decompose(a).getRight()));
		df.put("+pcons", f2((a, b) -> Tree.of(TermOp.AND___, a, b)));
		df.put("+pleft", f1(a -> Tree.decompose(a).getLeft()));
		df.put("+pright", f1(a -> Tree.decompose(a).getRight()));

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());
		IMap<Node, Fun<Frame, Node>> vm = IMap.empty();
		int fs = 0;
		Frame frame = new Frame(null);

		for (String key : keys) {
			vm = vm.put(Atom.of(key), getter(fs++));
			frame.add(df.get(key));
		}

		return eager0(fs, vm, parsed).apply(frame);
	}

	public void setLazyify(boolean isLazyify) {
		this.isLazyify = isLazyify;
	}

	private Fun<Frame, Node> eager0(int fs, IMap<Node, Fun<Frame, Node>> vm, Node node) {
		Fun<Frame, Node> result;
		Node m[];

		if ((m = Suite.matcher("APPLY .0 .1").apply(node)) != null) {
			Fun<Frame, Node> param_ = eager0(fs, vm, m[0]);
			Fun<Frame, Node> fun_ = eager0(fs, vm, m[1]);
			result = frame -> {
				Node fun = fun_.apply(frame);
				Node param = param_.apply(frame);
				return ((Fun_) fun).fun.apply(param);
			};
		} else if ((m = Suite.matcher("ATOM .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("BOOLEAN .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("CHARS .0").apply(node)) != null)
			result = immediate(new Data<>(Chars.of(((Str) m[0]).value)));
		else if ((m = Suite.matcher("CONS _ .0 .1").apply(node)) != null) {
			Fun<Frame, Node> p0_ = eager0(fs, vm, m[0]);
			Fun<Frame, Node> p1_ = eager0(fs, vm, m[1]);
			result = frame -> pair(p0_.apply(frame), p1_.apply(frame));
		} else if ((m = Suite.matcher("DECONS .0 .1 .2 .3 .4 .5").apply(node)) != null) {
			int fs1 = fs + 2;
			IMap<Node, Fun<Frame, Node>> vm1 = vm.put(m[2], getter(fs)).put(m[3], getter(fs + 1));
			Fun<Frame, Node> value_ = eager0(fs, vm, m[1]);
			Fun<Frame, Node> then_ = eager0(fs1, vm1, m[4]);
			Fun<Frame, Node> else_ = eager0(fs, vm, m[5]);
			Operator operator;

			if (m[0] == Atom.of("L"))
				operator = TermOp.AND___;
			else if (m[0] == Atom.of("P"))
				operator = TermOp.OR____;
			else
				throw new RuntimeException("Unknown DECONS type");

			result = frame -> {
				Tree tree = Tree.decompose(value_.apply(frame), operator);
				if (tree != null) {
					frame.add(tree.getLeft());
					frame.add(tree.getRight());
					return then_.apply(frame);
				} else
					return else_.apply(frame);
			};
		} else if ((m = Suite.matcher("DEF-VAR .0 .1 .2").apply(node)) != null) {
			IMap<Node, Fun<Frame, Node>> vm1 = vm.put(m[0], getter(fs));
			int fs1 = fs + 1;
			Fun<Frame, Node> value_ = eager0(fs1, vm1, m[1]);
			Fun<Frame, Node> expr = eager0(fs1, vm1, m[2]);
			result = frame -> {
				frame.add(value_.apply(frame));
				return expr.apply(frame);
			};
		} else if ((m = Suite.matcher("DEF-VARS .0 .1").apply(node)) != null) {
			Streamlet<Node[]> arrays = Tree.iter(m[0]).map(TreeUtil::tuple);
			List<Fun<Frame, Node>> values_ = new ArrayList<>();
			IMap<Node, Fun<Frame, Node>> vm1 = vm;
			int fs1 = fs;

			for (Node array[] : arrays) {
				Fun<Frame, Node> getter = getter(fs1);
				vm1 = vm1.put(array[0], frame -> ((Fun_) getter.apply(frame)).fun.apply(null));
				fs1++;
			}

			for (Node array[] : arrays) {
				Fun<Frame, Node> value_ = eager0(fs1, vm1, array[1]);
				values_.add(frame -> new Fun_(n -> value_.apply(frame)));
			}

			Fun<Frame, Node> expr = eager0(fs1, vm1, m[1]);

			result = frame -> {
				for (Fun<Frame, Node> value_ : values_)
					frame.add(value_.apply(frame));
				return expr.apply(frame);
			};
		} else if ((m = Suite.matcher("ERROR").apply(node)) != null)
			result = frame -> {
				throw new RuntimeException("Error termination");
			};
		else if ((m = Suite.matcher("FUN .0 .1").apply(node)) != null) {
			IMap<Node, Fun<Frame, Node>> vm1 = IMap.empty();
			for (Pair<Node, Fun<Frame, Node>> pair : vm) {
				Fun<Frame, Node> getter0 = pair.t1;
				vm1 = vm1.put(pair.t0, frame -> getter0.apply(frame.parent));
			}
			vm1 = vm1.put(m[0], getter(0));
			Fun<Frame, Node> value_ = eager0(fs + 1, vm1, m[1]);
			result = frame -> new Fun_(in -> {
				Frame frame1 = new Frame(frame);
				frame1.add(in);
				return value_.apply(frame1);
			});
		} else if ((m = Suite.matcher("IF .0 .1 .2").apply(node)) != null) {
			Fun<Frame, Node> if_ = eager0(fs, vm, m[0]);
			Fun<Frame, Node> then_ = eager0(fs, vm, m[1]);
			Fun<Frame, Node> else_ = eager0(fs, vm, m[2]);
			result = frame -> (if_.apply(frame) == Atom.TRUE ? then_ : else_).apply(frame);
		} else if ((m = Suite.matcher("NIL").apply(node)) != null)
			result = immediate(Atom.NIL);
		else if ((m = Suite.matcher("NUMBER .0").apply(node)) != null)
			result = immediate(m[0]);
		else if ((m = Suite.matcher("PRAGMA .0 .1").apply(node)) != null)
			result = eager0(fs, vm, m[1]);
		else if ((m = Suite.matcher("TCO .0 .1").apply(node)) != null) {
			Fun<Frame, Node> iter_ = eager0(fs, vm, m[0]);
			Fun<Frame, Node> in_ = eager0(fs, vm, m[1]);
			result = frame -> {
				Fun_ iter = (Fun_) iter_.apply(frame);
				Node in = in_.apply(frame);
				Tree p0, p1;
				do {
					Node out = iter.fun.apply(in);
					p0 = Tree.decompose(out, TermOp.AND___);
					p1 = Tree.decompose(p0.getRight(), TermOp.AND___);
					in = p1.getLeft();
				} while (p0.getLeft() != Atom.TRUE);
				return p1.getRight();
			};
		} else if ((m = Suite.matcher("TREE .0 .1 .2").apply(node)) != null)
			result = eager0(fs, vm, Suite.substitute("APPLY .2 APPLY .1 (VAR .0)", m[0], m[1], m[2]));
		else if ((m = Suite.matcher("UNWRAP .0").apply(node)) != null) {
			Fun<Frame, Node> value_ = eager0(fs, vm, m[0]);
			result = frame -> ((Wrap_) value_.apply(frame)).source.source();
		} else if ((m = Suite.matcher("VAR .0").apply(node)) != null)
			result = vm.get(m[0]);
		else if ((m = Suite.matcher("WRAP .0").apply(node)) != null) {
			Fun<Frame, Node> value_ = eager0(fs, vm, m[0]);
			result = frame -> new Wrap_(() -> value_.apply(frame));
		} else
			throw new RuntimeException("Unrecognized construct " + node);

		return result;
	}

	private IntrinsicCallback lazyIntrinsicCallback() {
		return new IntrinsicCallback() {
			public Node enclose(Intrinsic intrinsic, Node node) {
				return new Wrap_(() -> intrinsic.invoke(this, Arrays.asList(node)));
			}

			public Node yawn(Node node) {
				return ((Wrap_) node).source.source();
			}
		};
	}

	private Fun<Frame, Node> getter(int p) {
		return frame -> frame.get(p);
	}

	private Fun<Frame, Node> immediate(Node n) {
		return frame -> n;
	}

	private int compare(Node n0, Node n1) {
		int t0 = n0 instanceof Fun_ ? 1 : 0;
		int t1 = n1 instanceof Fun_ ? 1 : 0;
		int c = t0 - t1;
		if (c == 0)
			switch (t0) {
			case 0:
				c = Comparer.comparer.compare(n0, n1);
				break;
			case 1:
				c = System.identityHashCode(t0) - System.identityHashCode(t1);
			}
		return c;
	}

	private Node f1(Fun<Node, Node> fun) {
		return new Fun_(fun);
	}

	private Node f2(BiFunction<Node, Node, Node> fun) {
		return new Fun_(a -> new Fun_(b -> fun.apply(a, b)));
	}

	private Node fn(int n, Fun<List<Node>, Node> fun) {
		return fn(new ArrayList<>(), n, fun);
	}

	private Node fn(List<Node> ps, int n, Fun<List<Node>, Node> fun) {
		if (n != 0)
			return new Fun_(p -> {
				List<Node> ps1 = new ArrayList<>(ps);
				ps1.add(p);
				return fn(ps1, n - 1, fun);
			});
		else
			return fun.apply(ps);
	}

	private static Node pair(Node left, Node right) {
		return Tree.of(TermOp.AND___, left, right);
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Node thunk) {
		return ((Int) thunk).number;
	}

}
