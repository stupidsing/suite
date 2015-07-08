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
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class PredicateCallChecker {

	private List<Fun<Node, Node[]>> matchers = Arrays.asList( //
			Suite.matcher(".0, .1"), //
			Suite.matcher(".0; .1"), //
			Suite.matcher("find.all _ .0 _"), //
			Suite.matcher("if .0 then .1 else .2"), //
			Suite.matcher("not .0"), //
			Suite.matcher("once .0"));

	private Map<Prototype, Integer> nParametersByPrototype = new HashMap<>();

	public void check(List<Rule> rules) {
		for (Rule rule : rules)
			scan(rule.tail);
		// put(rule.head);
	}

	private Streamlet<Node> scan(Node node) {
		Node m[] = null;

		for (Fun<Node, Node[]> matcher : matchers)
			if (m == null)
				m = matcher.apply(node);

		if (m != null)
			return Read.from(m).concatMap(this::scan);
		else {
			if (Tree.decompose(node, TermOp.TUPLE_) != null || node instanceof Atom)
				put(node);
			return Read.empty();
		}
	}

	private void put(Node node) {
		Prototype prototype = Prototype.of(node);
		Integer np0;
		int np1 = getParameters(node);
		if ((np0 = nParametersByPrototype.get(prototype)) == null)
			nParametersByPrototype.put(prototype, np1);
		else if (np0 != np1)
			LogUtil.warn("Wrong number of parameters: " + prototype);
	}

	private int getParameters(Node node) {
		int n = 0;
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}
		return n;
	}

}
