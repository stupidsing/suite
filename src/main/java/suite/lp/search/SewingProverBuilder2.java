package suite.lp.search;

import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.kb.Prototype;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.impl.QueryRewriter;
import suite.node.Node;
import suite.streamlet.FunUtil.Fun;

public class SewingProverBuilder2 implements Builder {

	private ProverCfg proverCfg;

	public SewingProverBuilder2() {
		this(new ProverCfg());
	}

	public SewingProverBuilder2(ProverCfg proverCfg) {
		this.proverCfg = proverCfg;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		var isRewrite = !proverCfg.isTrace();
		var qr = isRewrite ? new QueryRewriter(Prototype.multimap(ruleSet)) : null;
		RuleSet ruleSet1;

		if (qr != null) {
			ruleSet1 = Suite.newRuleSet();
			qr.rules().entries().forEach(p -> ruleSet1.addRule(p.v));
		} else
			ruleSet1 = ruleSet;

		var fun = new SewingProverBuilder(proverCfg).build(ruleSet1);

		if (qr != null)
			return goal -> fun.apply(qr.rewriteClause(goal));
		else
			return fun;
	}

}
