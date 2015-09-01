package suite.lp.sewing.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.lp.checker.CheckerUtil;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.sewing.QueryRewriter;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tuple;
import suite.node.util.TreeUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Util;

/**
 * Converts query to tuple syntax for better performance.
 *
 * @author ywsing
 */
public class QueryRewriterImpl implements QueryRewriter {

	private CheckerUtil checkerUtil = new CheckerUtil();
	private Map<Prototype, PrototypeInfo> infosByPrototype;

	private class PrototypeInfo {
		private boolean isSkipFirst;
		private int length;

		private PrototypeInfo(Collection<Rule> rules) {
			Streamlet<Node> heads = Read.from(rules).map(rule -> rule.head);
			int n = heads.map(checkerUtil::getNumberOfParameters).min(Integer::compare);
			isSkipFirst = n > 0 && heads.isAll(head -> TreeUtil.getParameters(head, 1)[0] instanceof Atom);
			length = n - (isSkipFirst ? 1 : 0);
		}
	}

	public QueryRewriterImpl(ListMultimap<Prototype, Rule> rules) {
		infosByPrototype = Read.from(rules.listEntries()) //
				.map(Pair.map1(PrototypeInfo::new)) //
				.collect(As.map());
	}

	@Override
	public Node rewrite(Prototype prototype, Node node) {
		PrototypeInfo pi = infosByPrototype.get(prototype);
		int length = pi.length;

		if (length <= 0)
			return node;
		else {
			List<Node> ps = Arrays.asList(TreeUtil.getParameters(node, length));
			if (pi.isSkipFirst)
				ps = Util.right(ps, 1);
			return new Tuple(ps);
		}
	}

	@Override
	public Prototype getPrototype(Prototype prototype0, Node node, int n) {
		PrototypeInfo pi = infosByPrototype.get(prototype0);
		int n1 = n - (pi.isSkipFirst ? 1 : 0);
		return Prototype.of(((Tuple) node).nodes.get(n1));
	}

}
