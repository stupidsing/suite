import java.io.IOException;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.fp.FunCompilerConfig;
import suite.fp.eval.FunRbTreeTest;
import suite.instructionexecutor.InstructionTranslatorTest;
import suite.lp.doer.ProverConfig;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

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
		FunCompilerConfig fcc = new FunCompilerConfig();
		fcc.setNode(Int.create(1));

		Node node = Suite.substitute("" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ ", sink .out" //
		, Atom.create("LAZY"));

		ProverConfig pc = fcc.getProverConfig();
		Builder builder1 = new InterpretedProverBuilder(pc);
		Builder builder = CompiledProverBuilder.level1(pc, fcc.isDumpCode());
		Finder finder = builder.build(rs, compileNode);
		List<Node> nodes = collect(finder, appendLibraries(fcc));
		return nodes.size() == 1 ? nodes.get(0).finalNode() : null;
	}

}
