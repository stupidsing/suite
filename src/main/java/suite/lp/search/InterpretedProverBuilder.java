package suite.lp.search;

import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class InterpretedProverBuilder implements Builder {

	private ProverConfig proverConfig;

	public InterpretedProverBuilder() {
		this(new ProverConfig());
	}

	public InterpretedProverBuilder(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		return goal -> {
			var goal1 = SewingGeneralizerImpl.generalize(goal);

			return (source, sink) -> {
				var proverConfig1 = new ProverConfig(ruleSet, proverConfig);
				proverConfig1.setSource(source);
				proverConfig1.setSink(sink);
				new Prover(proverConfig1).elaborate(goal1);
			};
		};
	}

}
