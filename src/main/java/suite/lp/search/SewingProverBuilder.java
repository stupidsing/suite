package suite.lp.search;

import primal.fp.Funs.Fun;
import suite.lp.Configuration.ProverCfg;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.SewingProverImpl;
import suite.node.Node;

public class SewingProverBuilder implements Builder {

	private ProverCfg proverCfg;

	public SewingProverBuilder() {
		this(new ProverCfg());
	}

	public SewingProverBuilder(ProverCfg proverCfg) {
		this.proverCfg = proverCfg;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		var sewingProver = new SewingProverImpl(ruleSet);

		return goal -> {
			var goal1 = SewingGeneralizerImpl.generalize(goal);
			var pred = sewingProver.prover(goal1);

			return (source, sink) -> {
				var proverCfg1 = new ProverCfg(ruleSet, proverCfg);
				proverCfg1.setSource(source);
				proverCfg1.setSink(sink);
				pred.test(proverCfg1);
			};
		};
	}

}
