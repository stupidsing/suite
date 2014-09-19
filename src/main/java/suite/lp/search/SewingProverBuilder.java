package suite.lp.search;

import suite.lp.SewingProver;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class SewingProverBuilder implements Builder {

	private ProverConfig proverConfig;

	public SewingProverBuilder() {
		this(new ProverConfig());
	}

	public SewingProverBuilder(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(RuleSet ruleSet, Node goal) {
		Fun<ProverConfig, Boolean> fun = new SewingProver(ruleSet).compile(goal);
		ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);

		return (source, sink) -> {
			proverConfig1.setSource(source);
			proverConfig1.setSink(sink);
			fun.apply(proverConfig1);
		};
	}

}
