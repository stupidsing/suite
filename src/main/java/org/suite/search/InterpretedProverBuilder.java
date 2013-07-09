package org.suite.search;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.ProverBuilder.Builder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;

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
		final Node goal1 = new Generalizer().generalize(goal);
		final ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);

		return new Finder() {
			public void find(Source<Node> source, Sink<Node> sink) {
				proverConfig1.setSource(source);
				proverConfig1.setSink(sink);
				new Prover(proverConfig1).prove(goal1);
			}
		};
	}

}
