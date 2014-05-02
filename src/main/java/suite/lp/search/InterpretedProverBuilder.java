package suite.lp.search;

import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;

public class InterpretedProverBuilder implements Builder {

	private ProverConfig proverConfig;

	public InterpretedProverBuilder() {
		this(new ProverConfig());
	}

	public InterpretedProverBuilder(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(RuleSet ruleSet, Node goal) {
		Node goal1 = new Generalizer().generalize(goal);
		ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);

		return (source, sink) -> {
			proverConfig1.setSource(source);
			proverConfig1.setSink(sink);
			new Prover(proverConfig1).elaborate(goal1);
		};
	}

}
