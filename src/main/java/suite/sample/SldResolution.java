package suite.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import primal.Verbs.Concat;
import primal.fp.Funs.Source;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.search.CompiledProverBuilder;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.BaseOp;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;

/**
 * Selective linear definite clause resolution.
 *
 * @author ywsing
 */
public class SldResolution {

	private static Atom not = Atom.of("NOT");

	public List<Node> resolve(Node node) {
		var ruleSet = Suite.newRuleSet(List.of("auto.sl", "pt.sl"));
		var builder = CompiledProverBuilder.level1(new ProverCfg());
		var finder = builder.build(ruleSet).apply(Suite.parse("" //
				+ "source .n0" //
				+ ", pt-prove0 .n0 .n1" //
				+ ", pt-prove1 .n1 .n2" //
				+ ", pt-prove2 .n2 .n3" //
				+ ", pt-prove3 .n3 .n4" //
				+ ", pt-prove4 .n4 .n5" //
				+ ", pt-prove5 .n5 ()/.n6" //
				+ ", sink .n6"));

		var n0 = finder.collectSingle(node);
		var orsMap = new HashMap<Node, Source<List<Node>>>();

		for (var n1 : Tree.read(n0)) {
			var ors = Tree.read(n1).toList();

			for (var index = 0; index < ors.size(); index++) {
				var i = index;
				orsMap.put(ors.get(i), () -> Concat.lists(ors.subList(0, i), ors.subList(i + 1, ors.size())));
			}
		}

		var results = new ArrayList<Node>();

		for (var e : orsMap.entrySet()) {
			var value0 = e.getValue();
			var value1 = orsMap.get(negate(e.getKey()));

			if (value1 != null)
				results.add(TreeUtil.buildUp(BaseOp.AND___, Concat.lists(value0.g(), value1.g())));
		}

		return results;
	}

	private Node negate(Node key) {
		var tree = Tree.decompose(key, TermOp.TUPLE_);
		var isAlreadyNegated = tree != null && tree.getLeft() == not;
		return isAlreadyNegated ? tree.getRight() : Tree.of(TermOp.TUPLE_, not, key);
	}

}
