package suite.lp.sewing.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.ListMultimap;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;
import suite.util.Util;

/**
 * Converts query to tuple syntax for better performance.
 *
 * @author ywsing
 */
public class QueryRewriter {

	private Map<Prototype, PrototypeInfo> infoByPrototype;
	private ListMultimap<Prototype, Rule> rules1;

	private class PrototypeInfo {
		private boolean isSkipFirst;
		private int length;

		private PrototypeInfo(Collection<Rule> rules) {
			int n = Read.from(rules) //
					.map(rule -> TreeUtil.getNumberOfElements(rule.head)) //
					.min(Integer::compare);
			isSkipFirst = n > 0
					&& Read.from(rules).map(rule -> rule.head).isAll(head -> TreeUtil.getElements(head, 1)[0] instanceof Atom);
			length = n - (isSkipFirst ? 1 : 0);
		}
	}

	public QueryRewriter(ListMultimap<Prototype, Rule> rules) {
		infoByPrototype = Read.from(rules).mapValue(PrototypeInfo::new).toMap();
		rules1 = Read.multimap(rules).mapValue(this::rewriteRule).toMultimap();
	}

	private Rule rewriteRule(Rule rule) {
		return new Rule(rewriteQuery(rule.head), rewriteClause(rule.tail));
	}

	public Node rewriteClause(Node node0) {
		Node nodex;
		if ((nodex = rewriteClause(".0, .1", Arrays.asList(0, 1), node0)) != null //
				|| (nodex = rewriteClause(".0; .1", Arrays.asList(0, 1), node0)) != null//
				|| (nodex = rewriteClause("find.all .0 .1 .2", Arrays.asList(1), node0)) != null //
				|| (nodex = rewriteClause("find.all.memoized .0 .1 .2", Arrays.asList(1), node0)) != null //
				|| (nodex = rewriteClause("if .0 .1 .2", Arrays.asList(0, 1, 2), node0)) != null //
				|| (nodex = rewriteClause("list.fold .0 .1 .2", Arrays.asList(2), node0)) != null //
				|| (nodex = rewriteClause("list.query .0 .1 .2", Arrays.asList(2), node0)) != null //
				|| (nodex = rewriteClause("not .0", Arrays.asList(0), node0)) != null //
				|| (nodex = rewriteClause("once .0", Arrays.asList(0), node0)) != null //
				|| (nodex = rewriteClause("suspend .0 .1 .2", Arrays.asList(2), node0)) != null //
				|| (nodex = rewriteClause("try .0 .1 .2", Arrays.asList(0, 2), node0)) != null)
			;
		else if (Tree.decompose(node0, TermOp.TUPLE_) != null)
			nodex = rewriteQuery(node0);
		else
			nodex = node0;
		return nodex;
	}

	public ListMultimap<Prototype, Rule> rules() {
		return rules1;
	}

	private Node rewriteClause(String s, List<Integer> indices, Node node0) {
		Node m[];
		if ((m = Suite.matcher(s).apply(node0)) != null) {
			for (int i : indices)
				m[i] = rewriteClause(m[i]);
			return Suite.substitute(s, m);
		} else
			return null;
	}

	private Node rewriteQuery(Node node0) {
		Node nodex;
		Prototype prototype = Prototype.of(node0);
		PrototypeInfo pi;

		if ((pi = infoByPrototype.get(prototype)) != null) {
			int length = pi.length;

			if (length <= 0)
				nodex = node0;
			else {
				List<Node> ps = Arrays.asList(TreeUtil.getElements(node0, length));
				if (pi.isSkipFirst)
					ps = Util.right(ps, 1);
				nodex = Tuple.of(ps);
			}
		} else
			nodex = node0;

		return nodex;
	}

}
