package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.adt.pair.Pair;
import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
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
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.TreeUtil;
import suite.node.util.TreeUtil.IntInt_Bool;
import suite.primitive.IntInt_Int;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.BinOp;
import suite.util.To;

public class InterpretFunEager {

	private boolean isLazyify = false;

	private static class Fun_ extends Node {
		private Iterate<Node> fun;

		private Fun_(Iterate<Node> fun) {
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

	private class Eager_ {
		private int fs;
		private IMap<Node, Fun<Frame, Node>> vm;

		private Eager_(int fs, IMap<Node, Fun<Frame, Node>> vm) {
			this.fs = fs;
			this.vm = vm;
		}

		private Fun<Frame, Node> eager_(Node node) {
			Fun<Frame, Node> result;
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
				Fun<Frame, Node> param_ = eager_(APPLY.param);
				Fun<Frame, Node> fun_ = eager_(APPLY.fun);
				result = frame -> {
					Node fun = fun_.apply(frame);
					Node param = param_.apply(frame);
					return fun(fun).apply(param);
				};
			} else if ((ATOM = Matcher.atom.match(node)) != null)
				result = immediate(ATOM.value);
			else if ((BOOLEAN = Matcher.boolean_.match(node)) != null)
				result = immediate(BOOLEAN.value);
			else if ((CHARS = Matcher.chars.match(node)) != null)
				result = immediate(new Data<>(To.chars(((Str) CHARS.value).value)));
			else if ((CONS = Matcher.cons.match(node)) != null) {
				Fun<Frame, Node> p0_ = eager_(CONS.head);
				Fun<Frame, Node> p1_ = eager_(CONS.tail);
				Operator operator = oper(CONS.type);
				result = frame -> Tree.of(operator, p0_.apply(frame), p1_.apply(frame));
			} else if ((DECONS = Matcher.decons.match(node)) != null) {
				Fun<Frame, Node> value_ = eager_(DECONS.value);
				Fun<Frame, Node> then_ = put(DECONS.left).put(DECONS.right).eager_(DECONS.then);
				Fun<Frame, Node> else_ = eager_(DECONS.else_);
				Operator operator = oper(DECONS.type);
				result = frame -> {
					Tree tree = Tree.decompose(value_.apply(frame), operator);
					if (tree != null) {
						frame.add(tree.getLeft());
						frame.add(tree.getRight());
						return then_.apply(frame);
					} else
						return else_.apply(frame);
				};
			} else if ((DEFVARS = Matcher.defvars.match(node)) != null) {
				Match tuple = Suite.match(".0 .1");
				List<Node[]> arrays = Tree.iter(DEFVARS.list).map(tuple::apply).toList();
				if (arrays.size() == 1) {
					Node[] array = arrays.get(0);
					IMap<Node, Fun<Frame, Node>> vm1 = vm.put(array[0], unwrap(getter(fs)));
					Eager_ eager1 = new Eager_(fs + 1, vm1);
					Fun<Frame, Node> value_ = wrap(eager1.eager_(array[1]));
					Fun<Frame, Node> expr = eager1.eager_(DEFVARS.do_);
					result = frame -> {
						frame.add(value_.apply(frame));
						return expr.apply(frame);
					};
				} else {
					List<Fun<Frame, Node>> values_ = new ArrayList<>();
					IMap<Node, Fun<Frame, Node>> vm1 = vm;
					int fs1 = fs;

					for (Node[] array : arrays) {
						Fun<Frame, Node> getter = getter(fs1);
						vm1 = vm1.put(array[0], unwrap(getter));
						fs1++;
					}

					Eager_ eager1 = new Eager_(fs1, vm1);

					for (Node[] array : arrays)
						values_.add(wrap(eager1.eager_(array[1])));

					Fun<Frame, Node> expr = eager1.eager_(DEFVARS.do_);

					result = frame -> {
						for (Fun<Frame, Node> value_ : values_)
							frame.add(value_.apply(frame));
						return expr.apply(frame);
					};
				}
			} else if ((ERROR = Matcher.error.match(node)) != null)
				result = frame -> {
					throw new RuntimeException("error termination " + Formatter.display(ERROR.m));
				};
			else if ((FUN = Matcher.fun.match(node)) != null) {
				IMap<Node, Fun<Frame, Node>> vm1 = IMap.empty();
				for (Pair<Node, Fun<Frame, Node>> e : vm) {
					Fun<Frame, Node> getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}
				Fun<Frame, Node> value_ = new Eager_(0, vm1).put(FUN.param).eager_(FUN.do_);
				result = frame -> new Fun_(in -> {
					Frame frame1 = new Frame(frame);
					frame1.add(in);
					return value_.apply(frame1);
				});
			} else if ((IF = Matcher.if_.match(node)) != null) {
				Fun<Frame, Node> if_ = eager_(IF.if_);
				Fun<Frame, Node> then_ = eager_(IF.then_);
				Fun<Frame, Node> else_ = eager_(IF.else_);
				result = frame -> (if_.apply(frame) == Atom.TRUE ? then_ : else_).apply(frame);
			} else if (Matcher.nil.match(node) != null)
				result = immediate(Atom.NIL);
			else if ((NUMBER = Matcher.number.match(node)) != null)
				result = immediate(NUMBER.value);
			else if ((PRAGMA = Matcher.pragma.match(node)) != null)
				result = eager_(PRAGMA.do_);
			else if ((TCO = Matcher.tco.match(node)) != null) {
				Fun<Frame, Node> iter_ = eager_(TCO.iter);
				Fun<Frame, Node> in_ = eager_(TCO.in_);
				result = frame -> {
					Iterate<Node> iter = fun(iter_.apply(frame));
					Node in = in_.apply(frame);
					Tree p0, p1;
					do {
						Node out = iter.apply(in);
						p0 = Tree.decompose(out, TermOp.AND___);
						p1 = Tree.decompose(p0.getRight(), TermOp.AND___);
						in = p1.getLeft();
					} while (p0.getLeft() != Atom.TRUE);
					return p1.getRight();
				};
			} else if ((TREE = Matcher.tree.match(node)) != null)
				result = eager_(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", TREE.op, TREE.left, TREE.right));
			else if ((UNWRAP = Matcher.unwrap.match(node)) != null)
				result = unwrap(eager_(UNWRAP.do_));
			else if ((VAR = Matcher.var.match(node)) != null)
				result = vm.get(VAR.name);
			else if ((WRAP = Matcher.wrap.match(node)) != null)
				result = wrap(eager_(WRAP.do_));
			else
				throw new RuntimeException("unrecognized construct " + node);

			return result;
		}

		private Eager_ put(Node node) {
			return new Eager_(fs + 1, vm.put(node, getter(fs)));
		}
	}

	public Node eager(Node node) {
		Node mode = isLazyify ? Atom.of("LAZY") : Atom.of("EAGER");
		Node query = Suite.substitute("source .in, fc-process-function .0 .in .out, sink .out", mode);

		RuleSet rs = Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl"));
		Finder finder = new SewingProverBuilder2().build(rs).apply(query);
		Node parsed = FindUtil.collectSingle(finder, node);
		IntrinsicCallback ic = isLazyify ? lazyIntrinsicCallback() : Intrinsics.eagerIntrinsicCallback;

		Map<String, Node> df = new HashMap<>();
		df.put(TermOp.AND___.name, f2((a, b) -> Tree.of(TermOp.AND___, a, b)));
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

		for (Entry<Operator, IntInt_Bool> e : TreeUtil.boolOperations.entrySet()) {
			IntInt_Bool fun = e.getValue();
			df.put(e.getKey().getName(), f2((a, b) -> b(fun.apply(compare(a, b), 0))));
		}

		for (Entry<Operator, IntInt_Int> e : TreeUtil.intOperations.entrySet()) {
			IntInt_Int fun = e.getValue();
			df.put(e.getKey().getName(), f2((a, b) -> Int.of(fun.apply(i(a), i(b)))));
		}

		List<String> keys = df.keySet().stream().sorted().collect(Collectors.toList());
		Eager_ eager0 = new Eager_(0, IMap.empty());
		Frame frame = new Frame(null);

		for (String key : keys) {
			eager0 = eager0.put(Atom.of(key));
			frame.add(df.get(key));
		}

		return eager0.eager_(parsed).apply(frame);
	}

	public void setLazyify(boolean isLazyify) {
		this.isLazyify = isLazyify;
	}

	private IntrinsicCallback lazyIntrinsicCallback() {
		return new IntrinsicCallback() {
			public Node enclose(Intrinsic intrinsic, Node node) {
				return new Wrap_(() -> intrinsic.invoke(this, List.of(node)));
			}

			public Node yawn(Node node) {
				return ((Wrap_) node).source.source();
			}
		};
	}

	private Fun<Frame, Node> getter(int p) {
		return frame -> frame.get(p);
	}

	private Iterate<Node> fun(Node n) {
		return ((Fun_) n).fun;
	}

	private Fun<Frame, Node> wrap(Fun<Frame, Node> value_) {
		return frame -> new Wrap_(() -> value_.apply(frame));
	}

	private Fun<Frame, Node> unwrap(Fun<Frame, Node> getter) {
		return frame -> ((Wrap_) getter.apply(frame)).source.source();
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

	private Node f1(Iterate<Node> fun) {
		return new Fun_(fun);
	}

	private Node f2(BinOp<Node> fun) {
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

	private Operator oper(Node type) {
		Operator operator;
		if (type == Atom.of("L"))
			operator = TermOp.OR____;
		else if (type == Atom.of("P"))
			operator = TermOp.AND___;
		else
			throw new RuntimeException("unknown CONS type");
		return operator;
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Node thunk) {
		return ((Int) thunk).number;
	}

}
