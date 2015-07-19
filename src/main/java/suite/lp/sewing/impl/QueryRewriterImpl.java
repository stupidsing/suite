package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.sewing.QueryRewriter;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.TermOp;
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

	private Map<Prototype, PrototypeInfo> infosByPrototype;

	private class PrototypeInfo {
		private boolean isSkipFirst;
		private int length;

		private PrototypeInfo(Collection<Rule> rules) {
			Streamlet<Node> heads = Read.from(rules).map(rule -> rule.head);
			int n = heads.map(QueryRewriterImpl.this::getNumberOfParameters).min(Integer::compare);
			isSkipFirst = n > 0 && heads.isAll(head -> get(head, 1).get(0) instanceof Atom);
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
		else if (pi.isSkipFirst)
			return new Tuple(Util.right(get(node, length), 1));
		else
			return new Tuple(get(node, length));
	}

	@Override
	public Prototype getPrototype(Prototype prototype0, Node node, int n) {
		PrototypeInfo pi = infosByPrototype.get(prototype0);
		int n1 = n - (pi.isSkipFirst ? 1 : 0);
		return Prototype.of(((Tuple) node).nodes.get(n1));
	}

	private int getNumberOfParameters(Node node) {
		int n = 0;
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}
		return n;
	}

	private List<Node> get(Node node, int n) {
		List<Node> list = new ArrayList<>(n + 1);
		int i = 0;
		for (; i < n; i++) {
			Tree tree = Tree.decompose(node, TermOp.TUPLE_);
			list.add(tree.getLeft());
			node = tree.getRight();
		}
		list.add(node);
		return list;
	}

}
