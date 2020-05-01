package suite.lp.search;

import primal.Verbs.Take;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import suite.lp.doer.Cloner;
import suite.lp.kb.RuleSet;
import suite.node.Node;

import java.util.ArrayList;
import java.util.List;

import static primal.statics.Fail.fail;

public class ProverBuilder {

	public interface Builder {
		public Fun<Node, Finder> build(RuleSet ruleSet);
	}

	public interface Finder {
		public void find(Source<Node> source, Sink<Node> sink);

		public default Node collectSingle(Node in) {
			var list = collectList(in);
			var size = list.size();
			if (size == 1)
				return list.get(0);
			else
				return fail(0 < size ? "too many results" : "no result");
		}

		public default List<Node> collectList(Node in) {
			var nodes = new ArrayList<Node>();
			find(Take.from(in), node -> nodes.add(new Cloner().clone(node)));
			return nodes;
		}

		public default Source<Node> collect(Node in) {
			var source = Take.from(in);
			return FunUtil.suck(sink0 -> find(source, node -> sink0.f(new Cloner().clone(node))));
		}
	}

}
