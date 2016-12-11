package suite.lp;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.ProverBuilder.Builder;

public class LogicCompilerLevel2Test {

	// require tail recursion to work
	@Test
	public void test0() {
		RuleSet rs = Suite.getRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
				+ "sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #" //
				+ "sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #" //
				+ "sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #" //
		));

		Builder builder = CompiledProverBuilder.level2(new ProverConfig());
		Suite.evaluateLogic(builder, rs, "(), sink ()");
	}

	@Test
	public void test1() throws IOException {
		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl"));
		Builder builder = CompiledProverBuilder.level2(new ProverConfig());
		Suite.evaluateLogic(builder, rs, "(), sink ()");
	}

}
