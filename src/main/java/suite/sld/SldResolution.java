package suite.sld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.Suite;
import suite.lp.doer.ProverConfig;
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

/**
 * Selective linear definite clause resolution.
 * 
 * @author ywsing
 */
public class SldResolution {

	private static final Atom not = Atom.create("NOT");

	public Node resolve(final Node node) {
		CompiledProverBuilder builder = CompiledProverBuilder.level1(new ProverConfig(), false);

		Finder finder = builder.build(Suite.createRuleSet(Arrays.asList("prove-theorem.sl")) //
				, Suite.parse("source .n0" //
						+ ", pt-prove0 .n0 .n1" //
						+ ", pt-prove1 .n1 .n2" //
						+ ", pt-prove1a .n2 .n3" //
						+ ", pt-prove2 .n3 .n4" //
						+ ", pt-prove3 .n4 .n5" //
						+ ", pt-prove4 .n5 .n6" //
						+ ", sink .n6" //
				));

		final Node result[] = new Node[] { null };

		finder.find(FunUtil.source(node), new Sink<Node>() {
			public void sink(Node node) {
				result[0] = node;
			}
		});

		Node n0 = result[0];
		Map<Node, Source<List<Node>>> orsMap = new HashMap<>();

		for (Node n1 : Node.iter(TermOp.AND___, n0)) {
			final List<Node> ors = To.list(Node.iter(TermOp.OR____, n1));

			for (int i = 0; i < ors.size(); i++) {
				final int index = i;

				orsMap.put(ors.get(index), new Source<List<Node>>() {
					public List<Node> source() {
						List<Node> ors1 = new ArrayList<>();
						ors1.addAll(ors.subList(0, index));
						ors1.addAll(ors.subList(index + 1, ors.size()));
						return ors1;
					}
				});
			}
		}

		for (Entry<Node, Source<List<Node>>> entry : orsMap.entrySet()) {
			Node key = entry.getKey();
			Source<List<Node>> value0 = entry.getValue();
			Node negated = negate(key);

			Source<List<Node>> value1 = orsMap.get(negated);

			if (value1 != null) {
				List<Node> merged = new ArrayList<>();
				merged.addAll(value0.source());
				merged.addAll(value1.source());
				System.out.println(merged);
			}
		}

		return result[0];
	}

	private Node negate(Node key) {
		Tree tree;
		boolean isTuple = (tree = Tree.decompose(key, TermOp.TUPLE_)) != null;
		boolean isAlreadyNegated = isTuple && tree.getLeft() == not;
		return isAlreadyNegated ? tree.getRight() : Tree.create(TermOp.TUPLE_, not, key);
	}

}
