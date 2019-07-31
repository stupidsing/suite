package suite.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import primal.fp.Funs.Source;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.search.CompiledProverBuilder;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.util.List_;
import suite.util.To;

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
			var ors = To.list(Tree.read(n1));

			for (var i = 0; i < ors.size(); i++) {
				var index = i;

				orsMap.put(ors.get(index), () -> List_.concat(ors.subList(0, index), ors.subList(index + 1, ors.size())));
			}
		}

		var results = new ArrayList<Node>();

		for (var e : orsMap.entrySet()) {
			var value0 = e.getValue();
			var value1 = orsMap.get(negate(e.getKey()));

			if (value1 != null)
				results.add(TreeUtil.buildUp(TermOp.AND___, List_.concat(value0.g(), value1.g())));
		}

		return results;
	}

	private Node negate(Node key) {
		var tree = Tree.decompose(key, TermOp.TUPLE_);
		var isAlreadyNegated = tree != null && tree.getLeft() == not;
		return isAlreadyNegated ? tree.getRight() : Tree.of(TermOp.TUPLE_, not, key);
	}

}
