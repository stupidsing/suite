package suite.sld;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.Suite;
import suite.lp.doer.Cloner;
import suite.lp.doer.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

/**
 * Selective linear definite clause resolution.
 * 
 * @author ywsing
 */
public class SldResolution {

	private static final Atom not = Atom.create("NOT");

	public Node resolve(final Node node) {
		RuleSet ruleSet = Suite.createRuleSet(Arrays.asList("auto.sl", "prove-theorem.sl"));
		CompiledProverBuilder builder = CompiledProverBuilder.level1(new ProverConfig(), false);
		Finder finder = builder.build(ruleSet //
				, Suite.parse("source .n0" //
						+ ", pt-prove0 .n0 .n1" //
						+ ", pt-prove1 .n1 .n2" //
						+ ", pt-prove2 .n2 .n3" //
						+ ", pt-prove3 () .n3 .n4" //
						+ ", pt-prove4 .n4 .n5" //
						+ ", pt-prove5 .n5 ()/.n6" //
						+ ", sink .n6" //
				));

		final Node result[] = new Node[] { null };

		finder.find(FunUtil.source(node), new Sink<Node>() {
			public void sink(Node node) {
				result[0] = new Cloner().clone(node);
			}
		});

		Node n0 = result[0];
		Map<Node, Source<List<Node>>> orsMap = new HashMap<>();

		for (Node n1 : Node.iter(n0, TermOp.AND___)) {
			final List<Node> ors = To.list(Node.iter(n1, TermOp.AND___));

			for (int i = 0; i < ors.size(); i++) {
				final int index = i;

				orsMap.put(ors.get(index), new Source<List<Node>>() {
					public List<Node> source() {
						return Util.add(ors.subList(0, index), ors.subList(index + 1, ors.size()));
					}
				});
			}
		}

		for (Entry<Node, Source<List<Node>>> entry : orsMap.entrySet()) {
			Source<List<Node>> value0 = entry.getValue();
			Source<List<Node>> value1 = orsMap.get(negate(entry.getKey()));

			if (value1 != null)
				System.out.println(Util.add(value0.source(), value1.source()));
		}

		return result[0];
	}

	private Node negate(Node key) {
		Tree tree = Tree.decompose(key, TermOp.TUPLE_);
		boolean isAlreadyNegated = tree != null && tree.getLeft() == not;
		return isAlreadyNegated ? tree.getRight() : Tree.create(TermOp.TUPLE_, not, key);
	}

}
