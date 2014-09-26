package suite.lp.search;

import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class ProverBuilder {

	public interface Builder {
		public Fun<Node, Finder> build(RuleSet ruleSet);
	}

	public interface Finder {
		public void find(Source<Node> source, Sink<Node> sink);
	}

}
