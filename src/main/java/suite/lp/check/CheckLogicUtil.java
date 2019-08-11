package suite.lp.check;

import java.util.List;
import java.util.Map;

import primal.streamlet.Streamlet;
import suite.BindArrayUtil.Pattern;
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

public class CheckLogicUtil {

	private List<Pattern> patterns = List.of( //
			Suite.pattern(".0, .1"), //
			Suite.pattern(".0; .1"), //
			Suite.pattern("find.all _ .0 _"), //
			Suite.pattern("find.all.memoized _ .0 _"), //
			Suite.pattern("if .0 then .1 else .2"), //
			Suite.pattern("list.fold _ _ .0"), //
			Suite.pattern("list.query _ _ .0"), //
			Suite.pattern("not .0"), //
			Suite.pattern("once .0"), //
			Suite.pattern("suspend _ _ .0"), //
			Suite.pattern("try .0 _ .1"));

	public Streamlet<Node> scan(Node node) {
		Node[] m = null;

		for (var pattern : patterns)
			if ((m = pattern.match(node)) != null)
				return Read.from(m).concatMap(this::scan);

		if (Tree.decompose(node, TermOp.TUPLE_) != null || node instanceof Atom)
			return Read.each(node);
		else
			return Read.empty();
	}

	public Map<Prototype, Integer> getNumberOfElements(List<Rule> rules) {
		return Read.from(rules).groupBy(rule -> Prototype.of(rule.head), this::getNumberOfElements).toMap();
	}

	private Integer getNumberOfElements(Streamlet<Rule> rules) {
		return rules.collect(As.min(rule -> TreeUtil.nElements(rule.head)));
	}

}
