package suite.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FunUtil.Source;
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
		RuleSet ruleSet = Suite.newRuleSet(Arrays.asList("auto.sl", "pt.sl"));
		CompiledProverBuilder builder = CompiledProverBuilder.level1(new ProverConfig());
		Finder finder = builder.build(ruleSet)
				.apply(Suite.parse("" //
						+ "source .n0" //
						+ ", pt-prove0 .n0 .n1" //
						+ ", pt-prove1 .n1 .n2" //
						+ ", pt-prove2 .n2 .n3" //
						+ ", pt-prove3 .n3 .n4" //
						+ ", pt-prove4 .n4 .n5" //
						+ ", pt-prove5 .n5 ()/.n6" //
						+ ", sink .n6"));

		Node n0 = FindUtil.collectSingle(finder, node);
		Map<Node, Source<List<Node>>> orsMap = new HashMap<>();

		for (Node n1 : Tree.iter(n0, TermOp.AND___)) {
			List<Node> ors = To.list(Tree.iter(n1, TermOp.AND___));

			for (int i = 0; i < ors.size(); i++) {
				int index = i;

				orsMap.put(ors.get(index), () -> List_.concat(ors.subList(0, index), ors.subList(index + 1, ors.size())));
			}
		}

		List<Node> results = new ArrayList<>();

		for (Entry<Node, Source<List<Node>>> e : orsMap.entrySet()) {
			Source<List<Node>> value0 = e.getValue();
			Source<List<Node>> value1 = orsMap.get(negate(e.getKey()));

			if (value1 != null)
				results.add(Tree.of(TermOp.AND___, List_.concat(value0.source(), value1.source())));
		}

		return results;
	}

	private Node negate(Node key) {
		Tree tree = Tree.decompose(key, TermOp.TUPLE_);
		boolean isAlreadyNegated = tree != null && tree.getLeft() == not;
		return isAlreadyNegated ? tree.getRight() : Tree.of(TermOp.TUPLE_, not, key);
	}

}
