package org.suite.search;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.ProveSearch.Builder;
import org.suite.search.ProveSearch.Finder;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;

public class InterpretedProveBuilder implements Builder {

	private ProverConfig proverConfig;

	public InterpretedProveBuilder() {
		this(new ProverConfig());
	}

	public InterpretedProveBuilder(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(RuleSet ruleSet, Node goal) {
		final Node goal1 = new Generalizer().generalize(goal);
		final ProverConfig config = new ProverConfig(ruleSet, proverConfig);

		return new Finder() {
			public void find(Source<Node> source, Sink<Node> sink) {
				config.setSource(source);
				config.setSink(sink);
				new Prover(config).prove(goal1);
			}
		};
	}

}
