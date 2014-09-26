package suite.lp.search;

import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.SewingProver;
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
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		SewingProver sewingProver = new SewingProver(ruleSet);

		return goal -> {
			Fun<ProverConfig, Boolean> fun = sewingProver.compile(goal);

			return (source, sink) -> {
				ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);
				proverConfig1.setSource(source);
				proverConfig1.setSink(sink);
				fun.apply(proverConfig1);
			};
		};
	}

}
