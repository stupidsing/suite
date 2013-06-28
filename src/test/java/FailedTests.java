import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.instructionexecutor.FunRbTreeTest;
import org.instructionexecutor.InstructionTranslatorTest;
import org.junit.Test;
import org.suite.FunCompilerConfig;
import org.suite.Suite;
import org.suite.doer.Cloner;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel2;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Builder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;

public class FailedTests {

	// Need to increase InstructionExecutor.stackSize, or implement tail
	// recursion
	@Test
	public void test0() {
		RuleSet rs = Suite.nodeToRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
				+ "sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #" //
				+ "sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #" //
				+ "sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #" //
		));

		Node goal = Suite.parse("(), sink ()");
		Builder builder = new CompiledProverBuilderLevel2(new ProverConfig(), false);
		Suite.evaluateLogic(builder, rs, goal);
	}

	// Type check take 11 seconds
	@Test
	public void test1() throws IOException {
		new FunRbTreeTest().test();
	}

	// Strange error message "Unknown expression if b"
	@Test
	public void test2() throws IOException {
		Suite.evaluateFun("if a then b", false);
	}

	// Code too large
	@Test
	public void test3() throws IOException {
		new InstructionTranslatorTest().testStandardLibrary();
	}

	// Cannot resolve types... if we remove equals from fc.sl and fc-parse.sl
	@Test
	public void test4() throws IOException {
		String fp = "" //
				+ "define equals = (a => b => true) >> \n" //
				+ "define type (A %) of (cl,) >> \n" //
				+ "define type (B %) of (cl,) >> \n" //
				+ "(match \n" //
				+ "  || A % => error \n" //
				+ "  || B % => error \n" //
				+ "  || otherwise error \n" //
				+ ") {A %} \n";

		System.out.println(fp);

		FunCompilerConfig fcc = Suite.fcc(Suite.parse(fp));
		fcc.getLibraries().clear();

		Node node = Suite.parse("asserta (resolve-types .rt :- dump resolve-types .rt, nl, fail)" //
				+ ", source .in" //
				+ ", fc-parse .in .p" //
				+ ", infer-type-rule .p ()/()/() .tr/() .t" //
				+ ", resolve-types .tr" //
				+ ", fc-parse-type .out .t" //
				+ ", sink .out");

		Builder builder = new InterpretedProverBuilder(fcc.getProverConfig());
		Finder finder = builder.build(Suite.funCompilerRuleSet(), node);

		final Node type[] = new Node[] { null };

		Source<Node> source = FunUtil.source(fcc.getNode());

		Sink<Node> sink = new Sink<Node>() {
			public void sink(Node node) {
				type[0] = new Cloner().clone(node);
			}
		};

		finder.find(source, sink);

		assertNotNull(type[0]);
	}

}
