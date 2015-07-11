package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.TermOp;
import suite.streamlet.As;
import suite.streamlet.Read;

/**
 * Converts query to tuple syntax for better performance.
 *
 * @author ywsing
 */
public class QueryTupleUtil {

	private Map<Prototype, Integer> nParametersByPrototype;

	public QueryTupleUtil(ListMultimap<Prototype, Rule> rules) {
		nParametersByPrototype = Read.from(rules.listEntries()) //
				.map(Pair.map1(rules_ -> Read.from(rules_).map(rule -> getNumberOfParameters(rule.head)).min(Integer::compare))) //
				.collect(As.map());
	}

	public Node getTuple(Prototype prototype, Node node) {
		int nParameters = nParametersByPrototype.get(prototype);

		if (nParameters > 0) {
			List<Node> list = new ArrayList<>(nParameters);
			for (int i = 0; i < nParameters; i++) {
				Tree tree = Tree.decompose(node, TermOp.TUPLE_);
				list.add(tree.getLeft());
				node = tree.getRight();
			}
			list.add(node);
			return new Tuple(list);
		} else
			return node;
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

}
