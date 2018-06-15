package suite.fp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import suite.Suite;
import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.immutable.IMap;
import suite.lp.search.SewingProverBuilder2;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.BinOp;
import suite.util.To;

public class InterpretFunEager {

	private boolean isLazyify = false;

	private static class Fn extends Node {
		private Iterate<Node> fun;
	}

	private static class Wrap extends Node {
		private Source<Node> source;

		private Wrap(Source<Node> source) {
			this.source = source;
		}
	}

	private static class Frame extends ArrayList<Node> {
		private static final long serialVersionUID = 1l;
		private Frame parent;
	}

	private class Eager {
		private int fs;
		private IMap<Node, Fun<Frame, Node>> vm;

		private Eager(int fs, IMap<Node, Fun<Frame, Node>> vm) {
			this.fs = fs;
			this.vm = vm;
		}

		private Fun<Frame, Node> eager_(Node node) {
			return new SwitchNode<Fun<Frame, Node>>(node //
			).match(Matcher.apply, n -> {
				var param_ = eager_(n.param);
				var fun_ = eager_(n.fun);
				return frame -> {
					var fun = fun_.apply(frame);
					var param = param_.apply(frame);
					return fun(fun).apply(param);
				};
			}).match(Matcher.atom, n -> {
				return immediate(n.value);
			}).match(Matcher.boolean_, n -> {
				return immediate(n.value);
			}).match(Matcher.chars, n -> {
				return immediate(new Data<>(To.chars(((Str) n.value).value)));
			}).match(Matcher.cons, n -> {
				var p0_ = eager_(n.head);
				var p1_ = eager_(n.tail);
				var operator = oper(n.type);
				return frame -> Tree.of(operator, p0_.apply(frame), p1_.apply(frame));
			}).match(Matcher.decons, n -> {
				var value_ = eager_(n.value);
				var then_ = put(n.left).put(n.right).eager_(n.then);
				var else_ = eager_(n.else_);
				var operator = oper(n.type);
				return frame -> {
					var tree = Tree.decompose(value_.apply(frame), operator);
					if (tree != null) {
						frame.add(tree.getLeft());
						frame.add(tree.getRight());
						return then_.apply(frame);
					} else
						return else_.apply(frame);
				};
			}).match(Matcher.defvars, n -> {
				var tuple = Suite.pattern(".0 .1");
				var arrays = Tree.iter(n.list).map(tuple::match).toList();
				if (arrays.size() == 1) {
					var array = arrays.get(0);
					var vm1 = vm.put(array[0], unwrap(getter(fs)));
					var eager1 = new Eager(fs + 1, vm1);
					var value_ = wrap(eager1.eager_(array[1]));
					var expr = eager1.eager_(n.do_);
					return frame -> {
						frame.add(value_.apply(frame));
						return expr.apply(frame);
					};
				} else {
					var vm1 = vm;
					var fs1 = fs;

					for (var array : arrays) {
						var getter = getter(fs1++);
						vm1 = vm1.put(array[0], unwrap(getter));
					}

					var eager1 = new Eager(fs1, vm1);
					var values_ = Read.from(arrays).map(array -> wrap(eager1.eager_(array[1]))).toList();
					var expr = eager1.eager_(n.do_);

					return frame -> {
						for (var value_ : values_)
							frame.add(value_.apply(frame));
						return expr.apply(frame);
					};
				}
			}).match(Matcher.error, n -> {
				return frame -> Fail.t("error termination " + Formatter.display(n.m));
			}).match(Matcher.fun, FUN -> {
				var vm1 = IMap.<Node, Fun<Frame, Node>> empty();
				for (var e : vm) {
					var getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}
				var value_ = new Eager(0, vm1).put(FUN.param).eager_(FUN.do_);
				return frame -> f1(in -> {
					var frame1 = new Frame();
					frame1.parent = frame;
					frame1.add(in);
					return value_.apply(frame1);
				});
			}).match(Matcher.if_, n -> {
				var if_ = eager_(n.if_);
				var then_ = eager_(n.then_);
				var else_ = eager_(n.else_);
				return frame -> (if_.apply(frame) == Atom.TRUE ? then_ : else_).apply(frame);
			}).match(Matcher.nil, n -> {
				return immediate(Atom.NIL);
			}).match(Matcher.number, n -> {
				return immediate(n.value);
			}).match(Matcher.pragma, n -> {
				return eager_(n.do_);
			}).match(Matcher.tco, n -> {
				var iter_ = eager_(n.iter);
				var in_ = eager_(n.in_);
				return frame -> {
					var iter = fun(iter_.apply(frame));
					var in = in_.apply(frame);
					Tree p0, p1;
					do {
						var out = iter.apply(in);
						p0 = Tree.decompose(out, TermOp.AND___);
						p1 = Tree.decompose(p0.getRight(), TermOp.AND___);
						in = p1.getLeft();
					} while (p0.getLeft() != Atom.TRUE);
					return p1.getRight();
				};
			}).match(Matcher.tree, n -> {
				return eager_(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", n.op, n.left, n.right));
			}).match(Matcher.unwrap, n -> {
				return unwrap(eager_(n.do_));
			}).match(Matcher.var, n -> {
				return vm.get(n.name);
			}).match(Matcher.wrap, n -> {
				return wrap(eager_(n.do_));
			}).nonNullResult();
		}

		private Eager put(Node node) {
			return new Eager(fs + 1, vm.put(node, getter(fs)));
		}
	}

	public Node eager(Node node) {
		var mode = Atom.of(isLazyify ? "LAZY" : "EAGER");
		var query = Suite.substitute("source .in, fc-process-function .0 .in .out, sink .out", mode);

		var rs = Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl"));
		var finder = new SewingProverBuilder2().build(rs).apply(query);
		var parsed = finder.collectSingle(node);
		var ic = isLazyify ? lazyIntrinsicCallback() : Intrinsics.eagerIntrinsicCallback;

		var boolOpMap = Read //
				.from2(TreeUtil.boolOperations) //
				.<String, Node> map2((k, fun) -> k.name_(), (k, fun) -> f2((a, b) -> b(fun.apply(compare(a, b), 0)))) //
				.toMap();

		var intOpMap = Read //
				.from2(TreeUtil.intOperations) //
				.<String, Node> map2((k, fun) -> k.name_(), (k, fun) -> f2((a, b) -> Int.of(fun.apply(i(a), i(b))))) //
				.toMap();

		var df = new HashMap<String, Node>();
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
		df.putAll(boolOpMap);
		df.putAll(intOpMap);

		var keys = df.keySet().stream().sorted().collect(Collectors.toList());
		var eager0 = new Eager(0, IMap.empty());
		var frame = new Frame();

		for (var key : keys) {
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
				return new Wrap(() -> intrinsic.invoke(this, List.of(node)));
			}

			public Node yawn(Node node) {
				return ((Wrap) node).source.source();
			}
		};
	}

	private Fun<Frame, Node> getter(int p) {
		return frame -> frame.get(p);
	}

	private Iterate<Node> fun(Node n) {
		return ((Fn) n).fun;
	}

	private Fun<Frame, Node> wrap(Fun<Frame, Node> value_) {
		return frame -> new Wrap(() -> value_.apply(frame));
	}

	private Fun<Frame, Node> unwrap(Fun<Frame, Node> getter) {
		return frame -> ((Wrap) getter.apply(frame)).source.source();
	}

	private Fun<Frame, Node> immediate(Node n) {
		return frame -> n;
	}

	private int compare(Node n0, Node n1) {
		var t0 = n0 instanceof Fn ? 1 : 0;
		var t1 = n1 instanceof Fn ? 1 : 0;
		var c = t0 - t1;
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
		return f(fun);
	}

	private Node f2(BinOp<Node> fun) {
		return f(a -> f(b -> fun.apply(a, b)));
	}

	private Node fn(int n, Fun<List<Node>, Node> fun) {
		return new Object() {
			private Node fn(List<Node> ps, int n, Fun<List<Node>, Node> fun) {
				if (n != 0)
					return f(p -> {
						var ps1 = new ArrayList<>(ps);
						ps1.add(p);
						return fn(ps1, n - 1, fun);
					});
				else
					return fun.apply(ps);
			}
		}.fn(new ArrayList<>(), n, fun);
	}

	private Node f(Iterate<Node> fun) {
		var fn = new Fn();
		fn.fun = fun;
		return fn;
	}

	private Operator oper(Node type) {
		Operator operator;
		if (type == Atom.of("L"))
			operator = TermOp.OR____;
		else if (type == Atom.of("P"))
			operator = TermOp.AND___;
		else
			operator = Fail.t("unknown CONS type");
		return operator;
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Node thunk) {
		return ((Int) thunk).number;
	}

}
