package suite.lp.checker;

import java.util.List;
import java.util.Map;

import suite.BindArrayUtil.Match;
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

public class CheckerUtil {

	private List<Match> matchers = List.of( //
			Suite.match(".0, .1"), //
			Suite.match(".0; .1"), //
			Suite.match("find.all _ .0 _"), //
			Suite.match("find.all.memoized _ .0 _"), //
			Suite.match("if .0 then .1 else .2"), //
			Suite.match("list.fold _ _ .0"), //
			Suite.match("list.query _ _ .0"), //
			Suite.match("not .0"), //
			Suite.match("once .0"), //
			Suite.match("suspend _ _ .0"), //
			Suite.match("try .0 _ .1"));

	public Streamlet<Node> scan(Node node) {
		Node[] m = null;

		for (Match matcher : matchers)
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
