package suite.lp.search;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.Prototype;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.impl.QueryRewriter;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class SewingProverBuilder2 implements Builder {

	private ProverConfig proverConfig;

	public SewingProverBuilder2() {
		this(new ProverConfig());
	}

	public SewingProverBuilder2(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		QueryRewriter qr = new QueryRewriter(Prototype.multimap(ruleSet));

		RuleSet ruleSet1 = Suite.createRuleSet();
		qr.rules().entries().forEach(p -> ruleSet1.addRule(p.t1));

		Fun<Node, Finder> fun = new SewingProverBuilder(proverConfig).build(ruleSet1);
		return goal -> fun.apply(qr.rewriteClause(goal));
	}

}
