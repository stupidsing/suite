package suite.lp.search;

import java.util.ArrayList;
import java.util.List;

import suite.lp.doer.Cloner;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.To;

public class ProverBuilder {

	public interface Builder {
		public Fun<Node, Finder> build(RuleSet ruleSet);
	}

	public interface Finder {
		public void find(Source<Node> source, Sink<Node> sink);

		public default Node collectSingle(Node in) {
			var list = collectList(in);
			if (list.size() == 1)
				return list.get(0);
			else if (!list.isEmpty())
				return Fail.t("too many results");
			else
				return Fail.t("failure");
		}

		public default List<Node> collectList(Node in) {
			var nodes = new ArrayList<Node>();
			find(To.source(in), node -> nodes.add(new Cloner().clone(node)));
			return nodes;
		}

		public default Source<Node> collect(Node in) {
			var source = To.source(in);
			return FunUtil.suck(sink0 -> find(source, node -> sink0.sink(new Cloner().clone(node))));
		}
	}

}
