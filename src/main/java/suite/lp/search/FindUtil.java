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
	private static Source<Node> collect(final Finder finder, final Source<Node> in) {
		Sink<Sink<Node>> fun = new Sink<Sink<Node>>() {
			public void sink(final Sink<Node> sink0) {
				Sink<Node> sink1 = new Sink<Node>() {
					public void sink(Node node) {
						sink0.sink(new Cloner().clone(node));
					}
				};

				finder.find(in, sink1);
			}
		};

		return FunUtil.suck(fun);
	}

}
