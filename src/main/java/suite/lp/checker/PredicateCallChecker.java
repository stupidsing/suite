package suite.lp.checker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;

public class PredicateCallChecker {

	private Fun<Node, Node[]> m0 = Suite.matcher(".0, .1");
	private Fun<Node, Node[]> m1 = Suite.matcher(".0; .1");
	private Fun<Node, Node[]> m2 = Suite.matcher("find.all _ .0 _");
	private Fun<Node, Node[]> m3 = Suite.matcher("if .0 then .1 else .2");
	private Fun<Node, Node[]> m4 = Suite.matcher("not .0");
	private Fun<Node, Node[]> m5 = Suite.matcher("once .0");
	private List<Fun<Node, Node[]>> matchers = Arrays.asList(m0, m1, m2, m3, m4, m5);

	private Map<Prototype, Integer> nParametersByPrototype = new HashMap<>();

	public void check(List<Rule> rules) {
		for (Rule rule : rules) {
			put(rule.head);
			scan(rule.tail);
		}
	}

	private Streamlet<Node> scan(Node node) {
		node = node.finalNode();
		Node m[] = null;

		for (Fun<Node, Node[]> matcher : matchers)
			if (m == null)
				m = matcher.apply(node);

		if (m != null)
			return Read.from(m).concatMap(c -> scan(c));
		else {
			if (Tree.decompose(node, TermOp.TUPLE_) != null || node instanceof Atom)
				put(node);
			return new Streamlet<Node>(FunUtil.nullSource());
		}
	}

	private void put(Node node) {
		Prototype prototype = Prototype.of(node);
		Integer np0;
		int np1 = getParameters(node);
		if ((np0 = nParametersByPrototype.get(prototype)) != null)
			if (np0 == np1)
				nParametersByPrototype.put(prototype, np1);
			else
				LogUtil.warn("Wrong number of parameters: " + prototype);
	}

	private int getParameters(Node node) {
		int n = 0;
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			tree.getRight();
			n++;
		}
		return n;
	}

}
