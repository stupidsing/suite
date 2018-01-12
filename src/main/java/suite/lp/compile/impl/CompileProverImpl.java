package suite.lp.compile.impl;

import java.util.Map;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.ProverFactory;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.util.FunUtil.Iterate;

public class CompileProverImpl implements ProverFactory {

	private static FunFactory f = new FunFactory();
	private static FunExpr fail = f._false();
	private static FunExpr ok = f._true();

	@Override
	public Prove_ prover(Node node) {
		FunCreator<Prove_> fc = FunCreator.of(Prove_.class, false);

		return fc.create(new Iterate<>() {
			@SuppressWarnings("unused")
			private FunExpr pc;

			public FunExpr apply(FunExpr pc) {
				this.pc = pc;
				return f.declare(fail, flag -> f.seq(compile_(node, f.assign(flag, ok)), flag));
			}

			private FunExpr compile_(Node node, FunExpr cps) {
				return new SwitchNode<FunExpr>(node //
				).match(".0, .1", m -> {
					return compile_(m[0], compile_(m[1], cps));
				}).match(".0; .1", m -> {
					FunExpr fcps;
					if (Boolean.FALSE) {
						Runnable r = FunCreator.of(Runnable.class, false).create(() -> cps).apply(Map.ofEntries());
						fcps = f.object(r).invoke("run");
					} else
						fcps = cps;
					FunExpr f0 = compile_(m[0], fcps);
					FunExpr f1 = compile_(m[1], fcps);
					return f.seq(f0, f1);
				}).match("fail", m -> {
					return f._void();
				}).match("yes", m -> {
					return cps;
				}).nonNullResult();
			}
		}).apply(Map.ofEntries());
	}

}
