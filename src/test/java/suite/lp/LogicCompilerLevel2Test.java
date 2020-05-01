package suite.lp;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.search.CompiledProverBuilder;

import java.io.IOException;
import java.util.List;

public class LogicCompilerLevel2Test {

	// require tail recursion to work
	@Test
	public void test0() {
		var rs = Suite.getRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
				+ "sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #" //
				+ "sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #" //
				+ "sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #" //
		));

		var builder = CompiledProverBuilder.level2(new ProverCfg());
		Suite.evaluateLogic(builder, rs, "(), sink ()");
	}

	@Test
	public void test1() throws IOException {
		var rs = Suite.newRuleSet(List.of("auto.sl"));
		var builder = CompiledProverBuilder.level2(new ProverCfg());
		Suite.evaluateLogic(builder, rs, "(), sink ()");
	}

}
