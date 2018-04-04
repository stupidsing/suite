package suite.lp.compile.impl;

import java.util.Map;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.ProverFactory;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.util.FunUtil.Fun;

public class CompileProverImpl implements ProverFactory {

	private static FunFactory f = new FunFactory();
	private static FunExpr ok = f._true();

	@Override
	public Prove_ prover(Node node) {
		var rt = f.input();

		Fun<FunExpr, ProveRt> cf = cps -> FunCreator.of(ProveRt.class, false).create(rt_ -> cps).apply(Map.ofEntries());

		var compiled = new Object() {
			private FunExpr compile_(Node node, FunExpr cps) {
				return new SwitchNode<FunExpr>(node //
				).match(".0, .1", m -> {
					return compile_(m[0], compile_(m[1], cps));
				}).match(".0; .1", m -> {
					FunExpr cps1;
					if (Boolean.TRUE) {
						ProveRt proveRt_ = cf.apply(cps);
						cps1 = f.object(proveRt_).invoke("test", rt);
					} else
						cps1 = cps;
					FunExpr f0 = compile_(m[0], cps1);
					FunExpr f1 = compile_(m[1], cps1);
					return f.seq(f0, f1);
				}).match("fail", m -> {
					return f._void();
				}).match("yes", m -> {
					return cps;
				}).nonNullResult();
			}
		}.compile_(node, rt.fieldSet("ok", ok));

		ProveRt proveRt = cf.apply(compiled);

		return proverConfig -> {
			Runtime_ rt_ = new Runtime_();
			rt_.proverConfig = proverConfig;
			proveRt.test(rt_);
			return rt_.ok;
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
