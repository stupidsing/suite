package suite.lp.compile.impl;

import java.util.Map;

import primal.fp.Funs2.BinOp;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.Binder;
import suite.lp.doer.BinderFactory;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.SwitchNode;

public class CompileBinderImpl extends CompileClonerImpl implements BinderFactory {

	private static FunFactory f = new FunFactory();
	private static FunExpr fail = f._false();
	private static FunExpr ok = f._true();

	private boolean isBindTrees;

	public CompileBinderImpl() {
		this(true);
	}

	public CompileBinderImpl(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public Bind_ binder(Node node) {
		var fc = FunCreator.of(Bind_.class, false);

		return fc.create(new BinOp<>() {
			private FunExpr env, trail, b;

			public FunExpr apply(FunExpr bindEnv, FunExpr target) {
				this.env = bindEnv.field("env");
				this.trail = bindEnv.field("trail");
				return f.declare(ok, b -> {
					this.b = b;
					return compile_(node, target.invoke("finalNode"));
				});
			}

			private FunExpr compile_(Node node, FunExpr target) {
				var br = bind(f.object_(node, Node.class), target);
				var brc = bindClone(node, target);

				return new SwitchNode<FunExpr>(node //
				).applyIf(Atom.class, n -> {
					return f.ifEquals(target, f.object(node), ok, br);
				}).applyIf(Int.class, n -> {
					return f.ifInstance(Int.class, target, i -> f.ifEquals(i.field("number"), f.int_(n.number), ok, fail), br);
				}).applyIf(Reference.class, n -> {
					var ref = env.field("refs").index(f.int_(mapper().computeIndex(n)));
					return f.invokeStatic(Binder.class, "bindReference", ref, target, trail);
				}).applyIf(Str.class, n -> {
					return f.ifInstance(Str.class, target,
							s -> f.object(n.value).invoke("equals", s.field("value").cast_(Object.class)), br);
				}).applyTree((op, l, r) -> {
					return f.declare(f.invokeStatic(Tree.class, "decompose", target, f.object(op)),
							t -> f.ifNonNull(t, compile_(l, t.invoke("getLeft"), compile_(r, t.invoke("getRight"), ok)), brc));
				}).applyIf(Tuple.class, n -> {
					return f.ifInstance(Tuple.class, target, tuple -> f.declare(tuple.field("nodes"), targets -> {
						var nodes = n.nodes;
						var fe = ok;
						for (var i = 0; i < nodes.length; i++)
							fe = compile_(nodes[i], targets.index(f.int_(i)), fe);
						return f.if_(targets.length(), fe, brc);
					}), brc);
				}).applyIf(Node.class, n -> {
					var cloner = cloner(n);
					return f.invokeStatic(Binder.class, "bind", target, f.object(cloner).invoke("apply", env), trail);
				}).nonNullResult();
			}

			private FunExpr compile_(Node node, FunExpr target, FunExpr cps) {
				return f.assign(b, compile_(node, target), f.if_(b, cps, fail));
			}

			private FunExpr bindClone(Node node, FunExpr target) {
				if (isBindTrees)
					return bind(f.object(cloner(node)).apply(env), target);
				else
					return fail;
			}

			private FunExpr bind(FunExpr node, FunExpr target) {
				return f.ifInstanceAnd(Reference.class, target, ref -> f.seq(trail.invoke("addBind", ref, node), ok));
			}
		}).apply(Map.ofEntries());
	}

}
