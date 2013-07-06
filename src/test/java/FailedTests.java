import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.instructionexecutor.FunRbTreeTest;
import org.instructionexecutor.InstructionTranslatorTest;
import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel1;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel2;
import org.suite.search.ProverBuilder.Builder;

public class FailedTests {

	// Type check take 11 seconds
	@Test
	public void test0() throws IOException {
		new FunRbTreeTest().test();
	}

	// Strange error message "Unknown expression if b"
	@Test
	public void test1() throws IOException {
		Suite.evaluateFun("if a then b", false);
	}

	// Code too large
	@Test
	public void test2() throws IOException {
		new InstructionTranslatorTest().testStandardLibrary();
	}

	// Require tail recursion to work
	@Test
	public void test3() {
		RuleSet rs = Suite.nodeToRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
		));

		Node goal = Suite.parse("(), sink ()");
		Builder builder = new CompiledProverBuilderLevel2(new ProverConfig(), false);
		Suite.evaluateLogic(builder, rs, goal);
	}

	@Test
	public void test4() {
		RuleSet rs = Suite.nodeToRuleSet(Suite.parse("" //
				+ "cg-optimize-tail-calls .li0 .ri0 \n" //
				+ "    :- dump A, nl, cg-is-returning .li0 \n" //
				+ "# \n" //
				+ "cg-optimize-tail-calls (.inst, .insts0) (.inst, .insts1) \n" //
				+ "    :- dump B, nl, !, cg-optimize-tail-calls .insts0 .insts1 \n" //
				+ "# \n" //
				+ "cg-optimize-tail-calls () () # \n" //
				+ "\n" //
				+ "cg-is-returning (.inst, .insts) :- cg-is-skip .inst, !, cg-is-returning .insts # \n" //
				+ "cg-is-returning (_ RETURN, _) # \n" //
				+ "\n" //
				+ "cg-is-skip (_ LABEL) # \n" //
		));

		Node goal = Suite.parse("cg-optimize-tail-calls (_ CALL _, _ LABEL,) _, sink ()");
		Builder builder = new CompiledProverBuilderLevel1(new ProverConfig(), true);
		assertTrue(!Suite.evaluateLogic(builder, rs, goal).isEmpty());
	}

}
