package suite.lp.compile.impl;

import java.util.HashMap;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.BinderFactory;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.util.FunUtil2.BinOp;

public class CompileBinderImpl1 extends CompileClonerImpl implements BinderFactory {

	private static FunFactory f = new FunFactory();
	private static FunExpr false_ = f._false();
	private static FunExpr true_ = f._true();

	private boolean isBindTrees;

	public CompileBinderImpl1() {
		this(true);
	}

	public CompileBinderImpl1(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public Bind_ binder(Node node) {
		FunCreator<Bind_> fc = FunCreator.of(Bind_.class);

		return fc.create(new BinOp<>() {
			private FunExpr bindEnv, target, b;

			public FunExpr apply(FunExpr bindEnv, FunExpr target) {
				this.bindEnv = bindEnv;
				return f.declare(true_, b -> {
					this.b = b;
					return compile_(node, target, b);
				});
			}

			private FunExpr compile_(Node node, FunExpr target, FunExpr cps) {
				FunExpr b1;

				if (node instanceof Atom) {
					FunExpr h = f.ifInstanceAnd(Reference.class, target, //
							ref -> f.seq(bindEnv.field("trail").invoke("addBind", ref, f.object(node)), true_));
					b1 = f.ifEquals(target, f.object(node), true_, h);
				} else if (node instanceof Int) {
					int num = ((Int) node).number;
					b1 = f.ifInstanceAnd(Int.class, target, //
							i -> f.ifEquals(i.field("number"), f.int_(num), true_, false_));
				} else
					throw new RuntimeException();

				return f.assign(b, b1, f.if_(b, cps, false_));
			}
		}).apply(new HashMap<>());
	}

}
