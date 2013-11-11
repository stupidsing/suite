import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.fp.eval.FunRbTreeTest;
import suite.instructionexecutor.InstructionTranslatorTest;
import suite.lp.doer.Cloner;
import suite.lp.doer.ProverConfig;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class FailedTests {

	// Type check take 11 seconds
	@Test
	public void test0() throws IOException {
		new FunRbTreeTest().test();
	}

	// /tmp/suite.instructionexecutor.InstructionTranslator/suite/instructionexecutor/TranslatedRun88.java:1034:
	// error: code too large
	// public Node exec(TranslatedRunConfig config, Closure closure) {
	@Test
	public void test1() throws IOException {
		new InstructionTranslatorTest().testStandardLibrary();
	}

	// UnsupportedOperationException
	@Test
	public void test2() throws IOException {
		new InstructionTranslatorTest().testAtomString();
	}

	// Cyclic types
	@Test
	public void test3() {
		Suite.evaluateFunType("define f = (v => (v;) = v) >> f");
	}

	// Functional compilation Goal failed
	@Test
	public void test4() {
		Node node = Suite.substitute("" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ ", sink .out" //
		, Atom.create("LAZY"));

		ProverConfig pc = new ProverConfig();
		Builder builder = CompiledProverBuilder.level1(pc, false);
		Finder finder = builder.build(Suite.createRuleSet(Arrays.asList("auto.sl", "fc.sl")), node);
		final List<Node> nodes = new ArrayList<>();

		Source<Node> source = FunUtil.source((Node) Int.create(1));
		Sink<Node> sink = new Sink<Node>() {
			public void sink(Node node) {
				nodes.add(new Cloner().clone(node));
			}
		};

		finder.find(source, sink);
		System.out.println(nodes.size() == 1 ? nodes.get(0).finalNode() : null);
	}

}
