package org.suite.search;

import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;

public class ProverBuilder {

	public interface Builder {
		public Finder build(RuleSet ruleSet, Node goal);
	}

	public interface Finder {
		public void find(Source<Node> source, Sink<Node> sink);
	}

}
