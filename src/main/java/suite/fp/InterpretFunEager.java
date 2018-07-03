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
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOr;
import suite.node.util.Comparer;
import suite.node.util.TreeUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.BinOp;
import suite.streamlet.Read;
import suite.util.Fail;
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
			).match(Matcher.apply, (param, fun) -> {
				var param_ = eager_(param);
				var fun_ = eager_(fun);
				return frame -> fun(fun_.apply(frame)).apply(param_.apply(frame));
			}).match(Matcher.atom, value -> {
				return immediate(value);
			}).match(Matcher.boolean_, value -> {
				return immediate(value);
			}).match(Matcher.chars, value -> {
				return immediate(new Data<>(To.chars(Str.str(value))));
			}).match(Matcher.cons, (type, head, tail) -> {
				var p0_ = eager_(head);
				var p1_ = eager_(tail);
				var operator = oper(type);
				return frame -> Tree.of(operator, p0_.apply(frame), p1_.apply(frame));
			}).match(Matcher.decons, (type, value, l, r, then, else_) -> {
				var valueFun = eager_(value);
				var thenFun = put(l).put(r).eager_(then);
				var elseFun = eager_(else_);
				var operator = oper(type);
				return frame -> {
					var tree = Tree.decompose(valueFun.apply(frame), operator);
					if (tree != null) {
						frame.add(tree.getLeft());
						frame.add(tree.getRight());
						return thenFun.apply(frame);
					} else
						return elseFun.apply(frame);
				};
			}).match(Matcher.defvars, (list, do_) -> {
				var tuple = Suite.pattern(".0 .1");
				var arrays = Tree.iter(list).map(tuple::match).toList();
				var vm1 = vm;
				var fs1 = fs;

				for (var array : arrays)
					vm1 = vm1.put(array[0], unwrap(getter(fs1++)));

				var eager1 = new Eager(fs1, vm1);
				var expr = eager1.eager_(do_);
				var values_ = Read.from(arrays).map(array -> wrap(eager1.eager_(array[1]))).toList();

				if (1 < arrays.size())
					return frame -> {
						for (var value_ : values_)
							frame.add(value_.apply(frame));
						return expr.apply(frame);
					};
				else {
					var value_ = values_.get(0);
					return frame -> {
						frame.add(value_.apply(frame));
						return expr.apply(frame);
					};
				}
			}).match(Matcher.error, m -> {
				return frame -> Fail.t("error termination " + Formatter.display(m));
			}).match(Matcher.fun, (param, do_) -> {
				var vm1 = IMap.<Node, Fun<Frame, Node>> empty();
				for (var e : vm) {
					var getter0 = e.t1;
					vm1 = vm1.put(e.t0, frame -> getter0.apply(frame.parent));
				}
				var value_ = new Eager(0, vm1).put(param).eager_(do_);
				return frame -> f1(in -> {
					var frame1 = new Frame();
					frame1.parent = frame;
					frame1.add(in);
					return value_.apply(frame1);
				});
			}).match(Matcher.if_, (if_, then, else_) -> {
				var ifFun = eager_(if_);
				var thenFun = eager_(then);
				var elseFun = eager_(else_);
				return frame -> (ifFun.apply(frame) == Atom.TRUE ? thenFun : elseFun).apply(frame);
			}).match(Matcher.nil, () -> {
				return immediate(Atom.NIL);
			}).match(Matcher.number, value -> {
				return immediate(value);
			}).match(Matcher.pragma, do_ -> {
				return eager_(do_);
			}).match(Matcher.tco, (iter_, in_) -> {
				var iterFun = eager_(iter_);
				var inFun = eager_(in_);
				return frame -> {
					var iter = fun(iterFun.apply(frame));
					var in = inFun.apply(frame);
					Tree p0, p1;
					do {
						var out = iter.apply(in);
						p0 = Tree.decompose(out, TermOp.AND___);
						p1 = Tree.decompose(p0.getRight(), TermOp.AND___);
						in = p1.getLeft();
					} while (p0.getLeft() != Atom.TRUE);
					return p1.getRight();
				};
			}).match(Matcher.tree, (op, left, right) -> {
				return eager_(Suite.substitute("APPLY .2 (APPLY .1 (VAR .0))", op, left, right));
			}).match(Matcher.unwrap, do_ -> {
				return unwrap(eager_(do_));
			}).match(Matcher.var, name -> {
				return vm.get(name);
			}).match(Matcher.wrap, do_ -> {
				return wrap(eager_(do_));
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
				.<String, Node> map2((k, fun) -> k.name_(), (k, fun) -> f2((a, b) -> Int.of(fun.apply(Int.num(a), Int.num(b))))) //
				.toMap();

		var df = new HashMap<String, Node>();
		df.put(TermOp.AND___.name, f2(TreeAnd::of));
		df.put("+call%i-t1", f1(i -> fn(1, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-t2", f1(i -> fn(2, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-t3", f1(i -> fn(3, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-v1", f1(i -> fn(1, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-v2", f1(i -> fn(2, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+call%i-v3", f1(i -> fn(3, l -> Data.<Intrinsic> get(i).invoke(ic, l))));
		df.put("+compare", f2((a, b) -> Int.of(Comparer.comparer.compare(a, b))));
		df.put("+get%i", f1(a -> new Data<>(Intrinsics.intrinsics.get(Atom.name(a).split("!")[1]))));
		df.put("+is-list", f1(a -> b(Tree.decompose(a) != null)));
		df.put("+is-pair", f1(a -> b(Tree.decompose(a) != null)));
		df.put("+lcons", f2(TreeOr::of));
		df.put("+lhead", f1(a -> Tree.decompose(a).getLeft()));
		df.put("+ltail", f1(a -> Tree.decompose(a).getRight()));
		df.put("+pcons", f2((a, b) -> TreeAnd.of(a, b)));
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
			if (t0 == 0)
				c = Comparer.comparer.compare(n0, n1);
			else
				c = System.identityHashCode(t0) - System.identityHashCode(t1);
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
				return n != 0 ? f(p -> {
					var ps1 = new ArrayList<>(ps);
					ps1.add(p);
					return fn(ps1, n - 1, fun);
				}) : fun.apply(ps);
			}
		}.fn(new ArrayList<>(), n, fun);
	}

	private Node f(Iterate<Node> fun) {
		var fn = new Fn();
		fn.fun = fun;
		return fn;
	}

	private Operator oper(Node type) {
		if (type == Atom.of("L"))
			return TermOp.OR____;
		else if (type == Atom.of("P"))
			return TermOp.AND___;
		else
			return Fail.t("unknown CONS type");
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

}
