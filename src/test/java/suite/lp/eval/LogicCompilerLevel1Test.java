package suite.lp.eval;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.Cloner;
import suite.lp.doer.ProverConfig;
import suite.lp.doer.Specializer;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Node;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class LogicCompilerLevel1Test {

	/**
	 * Compiles the functional compiler and use it to compile a simple
	 * functional program.
	 */
	@Test
	public void test() {
		Node program = Suite.parse("1 + 2");

		Node node = new Specializer().specialize(Suite.substitute("" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ ", sink .out" //
		, Atom.create("LAZY")));

		Builder builder = CompiledProverBuilder.level1(new ProverConfig(), false);
		Finder finder = builder.build(Suite.createRuleSet(Arrays.asList("auto.sl", "fc.sl")), node);
		final List<Node> nodes = new ArrayList<>();

		Source<Node> source = FunUtil.source(program);
		Sink<Node> sink = new Sink<Node>() {
			public void sink(Node node) {
				nodes.add(new Cloner().clone(node));
			}
		};

		finder.find(source, sink);

		Node result = nodes.size() == 1 ? nodes.get(0).finalNode() : null;
		System.out.println(result);
		assertNotNull(result);
	}

}
