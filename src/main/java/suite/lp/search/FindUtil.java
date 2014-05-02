package suite.lp.search;

import java.util.List;

import suite.lp.doer.Cloner;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.To;

public class FindUtil {

	public static Node collectSingle(Finder finder, Node in) {
		List<Node> list = To.list(collect(finder, in));
		if (list.size() == 1)
			return list.get(0).finalNode();
		else if (!list.isEmpty())
			throw new RuntimeException("Too many results");
		else
			throw new RuntimeException("Failure");
	}

	public static List<Node> collectList(Finder finder, Node in) {
		return To.list(collect(finder, in));
	}

	private static Source<Node> collect(Finder finder, Node in) {
		return collect(finder, To.source(in));
	}

	/**
	 * Does find in background.
	 */
	private static Source<Node> collect(Finder finder, Source<Node> in) {
		Sink<Sink<Node>> fun = sink0 -> {
			finder.find(in, node -> sink0.sink(new Cloner().clone(node)));
		};

		return FunUtil.suck(fun);
	}

}
