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
			Suite.matcher("once .0"), //
			Suite.matcher("try .0 _ .1"));

	private Map<Prototype, Integer> nParametersByPrototype = new HashMap<>();

	public void check(List<Rule> rules) {
		Read.from(rules).concatMap(rule -> scan(rule.head)).forEach(this::putHead);
		Read.from(rules).concatMap(rule -> scan(rule.tail)).forEach(this::putTail);
	}

	private Streamlet<Node> scan(Node node) {
		Node m[] = null;

		for (Fun<Node, Node[]> matcher : matchers)
			if ((m = matcher.apply(node)) != null)
				return Read.from(m).concatMap(this::scan);

		if (Tree.decompose(node, TermOp.TUPLE_) != null || node instanceof Atom)
			return Read.from(node);
		else
			return Read.empty();
	}

	private void putHead(Node node) {
		Prototype prototype = Prototype.of(node);
		int np = getParameters(node);
		nParametersByPrototype.compute(prototype, (p, np0) -> np0 != null ? Math.min(np0, np) : np);
	}

	private void putTail(Node node) {
		Prototype prototype = Prototype.of(node);
		Integer np0 = nParametersByPrototype.get(prototype);
		int np1 = getParameters(node);
		if (np0 != null && np0 > np1)
			LogUtil.warn("Not enough number of parameters: " + prototype);
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
