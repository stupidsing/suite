package org.suite.search;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.doer.Station;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.suite.search.ProveSearch.Builder;
import org.suite.search.ProveSearch.Finder;
import org.util.FunUtil.Sink;

public class InterpretedProveBuilder implements Builder {

	private ProverConfig proverConfig;

	public InterpretedProveBuilder(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(RuleSet rs, Node goal) {
		Generalizer generalizer = new Generalizer();
		final Node goal1 = generalizer.generalize(goal);
		final Reference in1 = generalizer.getVariable(in);
		final Reference out1 = generalizer.getVariable(out);
		final Prover prover = new Prover(new ProverConfig(rs, proverConfig));

		return new Finder() {
			public void find(Node in, final Sink<Node> sink) {
				in1.bound(in);

				prover.prove(Tree.create(TermOp.AND___, goal1, new Station() {
					public boolean run() {
						sink.sink(out1);
						return false;
					}
				}));
			}
		};
	}

}
