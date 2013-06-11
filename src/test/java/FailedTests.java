import java.io.IOException;

import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.Formatter;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel2;
import org.suite.search.ProverBuilder.Builder;
import org.util.IoUtil;

public class FailedTests {

	@Test
	public void test0() { // takes very long
		Suite.evaluateEagerFun("" //
				+ "define type (A %) of (t,) >> \n" //
				+ "define type (B %) of (t,) >> \n" //
				+ "define type (C %) of (t,) >> ( \n" //
				+ "    ((A %):1:, (A %):2:,), \n" //
				+ "    ((B %):1:, (B %):2:,), \n" //
				+ "    ((C %):1:, (C %):2:,), \n" //
				+ ")");
	}

	// need to increase InstructionExecutor.stackSize, or implement tail
	// recursion
	@Test
	public void test1() {
		RuleSet rs = Suite.nodeToRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
				+ "sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #" //
				+ "sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #" //
				+ "sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #" //
		));

		Node goal = Suite.parse("(), sink ()");
		Builder builder = new CompiledProverBuilderLevel2(new ProverConfig(), false);
		Suite.evaluateLogical(builder, rs, goal);
	}

	// Runs forever!
	@Test
	public void test2() throws IOException {
		String s = IoUtil.readStream(getClass().getResourceAsStream("/RB-TREE.slf"));
		String fp = s + "0 until 10 | map {add} | apply | {EMPTY %}\n";
		Node result = Suite.evaluateEagerFun(fp);
		System.out.println("OUT:\n" + Formatter.dump(result));
	}

	// Strange error message "Unknown expression if b"
	@Test
	public void test3() throws IOException {
		Suite.evaluateEagerFun("if a then b");
	}

}
