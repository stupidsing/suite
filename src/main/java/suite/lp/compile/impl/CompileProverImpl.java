package suite.lp.compile.impl;

import java.util.Map;

import primal.fp.Funs.Fun;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.Configuration.ProverCfg;
import suite.lp.doer.ProverFactory;
import suite.node.Node;
import suite.node.io.SwitchNode;

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
				).matchArray(".0, .1", m -> {
					return compile_(m[0], compile_(m[1], cps));
				}).matchArray(".0; .1", m -> {
					FunExpr cps1;
					if (Boolean.TRUE) {
						var proveRt_ = cf.apply(cps);
						cps1 = f.object(proveRt_).invoke("test", rt);
					} else
						cps1 = cps;
					FunExpr f0 = compile_(m[0], cps1);
					FunExpr f1 = compile_(m[1], cps1);
					return f.seq(f0, f1);
				}).matchArray("fail", m -> {
					return f._void();
				}).matchArray("yes", m -> {
					return cps;
				}).nonNullResult();
			}
		}.compile_(node, rt.fieldSet("ok", ok));

		var proveRt = cf.apply(compiled);

		return proverCfg -> {
			var rt_ = new Runtime_();
			rt_.proverCfg = proverCfg;
			proveRt.test(rt_);
			return rt_.ok;
		};
	}

	public interface ProveRt {
		public void test(Runtime_ rt);
	}

	public static class Runtime_ {
		public ProverCfg proverCfg;
		public boolean ok;
	}

}
