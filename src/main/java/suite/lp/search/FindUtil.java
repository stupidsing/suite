package suite.lp.search;

import java.util.ArrayList;
import java.util.List;

import suite.lp.doer.Cloner;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.To;

public class FindUtil {

	public static Node collectSingle(Finder finder, Node in) {
		var list = collectList(finder, in);
		if (list.size() == 1)
			return list.get(0);
		else if (!list.isEmpty())
			return Fail.t("too many results");
		else
			return Fail.t("failure");
	}

	public static List<Node> collectList(Finder finder, Node in) {
		List<Node> nodes = new ArrayList<>();
		finder.find(To.source(in), node -> nodes.add(new Cloner().clone(node)));
		return nodes;
	}

	public static Source<Node> collect(Finder finder, Node in) {
		return collect(finder, To.source(in));
	}

	/**
	 * Does find in background.
	 */
	private static Source<Node> collect(Finder finder, Source<Node> in) {
		return FunUtil.suck(sink0 -> finder.find(in, node -> sink0.sink(new Cloner().clone(node))));
	}

}
