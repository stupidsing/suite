package suite.lp.compile.impl;

import java.util.Map;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.ProverFactory;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.util.FunUtil.Iterate;

public class CompileProverImpl implements ProverFactory {

	private static FunFactory f = new FunFactory();
	private static FunExpr ok = f._true();

	@Override
	public Prove_ prover(Node node) {
		ProveRt proveRt = FunCreator.of(ProveRt.class, false).create(rt -> {
			return new Object() {
				private Iterate<FunExpr> compile_(Node node, Iterate<FunExpr> cps) {
					return new SwitchNode<Iterate<FunExpr>>(node //
					).match(".0, .1", m -> {
						return compile_(m[0], compile_(m[1], cps));
					}).match(".0; .1", m -> {
						Iterate<FunExpr> fcps;
						if (Boolean.TRUE) {
							ProveRt r = FunCreator.of(ProveRt.class, false).create(cps).apply(Map.ofEntries());
							fcps = rt_ -> f.object(r).invoke("test", rt_);
						} else
							fcps = cps;
						Iterate<FunExpr> f0 = compile_(m[0], fcps);
						Iterate<FunExpr> f1 = compile_(m[1], fcps);
						return rt_ -> f.seq(f0.apply(rt_), f1.apply(rt_));
					}).match("fail", m -> {
						return rt_ -> f._void();
					}).match("yes", m -> {
						return cps;
					}).nonNullResult();
				}
			}.compile_(node, rt_ -> rt_.fieldSet("ok", ok)).apply(rt);
		}).apply(Map.ofEntries());

		return proverConfig -> {
			Runtime_ rt = new Runtime_();
			rt.proverConfig = proverConfig;
			proveRt.test(rt);
			return rt.ok;
		};
	}

	public interface ProveRt {
		public void test(Runtime_ rt);
	}

	public static class Runtime_ {
		public ProverConfig proverConfig;
		public boolean ok;
	}

}
