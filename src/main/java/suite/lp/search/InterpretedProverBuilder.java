package suite.lp.search;

import suite.lp.Configuration.ProverCfg;
import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Node;
import suite.streamlet.FunUtil.Fun;

public class InterpretedProverBuilder implements Builder {

	private ProverCfg proverCfg;

	public InterpretedProverBuilder() {
		this(new ProverCfg());
	}

	public InterpretedProverBuilder(ProverCfg proverCfg) {
		this.proverCfg = proverCfg;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		return goal -> {
			var goal1 = SewingGeneralizerImpl.generalize(goal);

			return (source, sink) -> {
				var proverCfg1 = new ProverCfg(ruleSet, proverCfg);
				proverCfg1.setSource(source);
				proverCfg1.setSink(sink);
				new Prover(proverCfg1).elaborate(goal1);
			};
		};
	}

}
