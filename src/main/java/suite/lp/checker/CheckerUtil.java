package suite.lp.checker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class CheckerUtil {

	private List<Fun<Node, Node[]>> matchers = Arrays.asList( //
			Suite.matcher(".0, .1"), //
			Suite.matcher(".0; .1"), //
			Suite.matcher("find.all _ .0 _"), //
			Suite.matcher("find.all.memoized _ .0 _"), //
			Suite.matcher("if .0 then .1 else .2"), //
			Suite.matcher("list.fold _ _ .0"), //
			Suite.matcher("list.query _ _ .0"), //
			Suite.matcher("not .0"), //
			Suite.matcher("once .0"), //
			Suite.matcher("suspend _ _ .0"), //
			Suite.matcher("try .0 _ .1"));

	public Streamlet<Node> scan(Node node) {
		Node[] m = null;

		for (Fun<Node, Node[]> matcher : matchers)
			if ((m = matcher.apply(node)) != null)
				return Read.from(m).concatMap(this::scan);

		if (Tree.decompose(node, TermOp.TUPLE_) != null || node instanceof Atom)
			return Read.each(node);
		else
			return Read.empty();
	}

	public Map<Prototype, Integer> getNumberOfElements(List<Rule> rules) {
		return Read.from(rules).groupBy(rule -> Prototype.of(rule.head), this::getNumberOfElements).collect(As::map);
	}

	private Integer getNumberOfElements(Streamlet<Rule> rules) {
		return rules.collect(As.min(rule -> TreeUtil.nElements(rule.head)));
	}

}
