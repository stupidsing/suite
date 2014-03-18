package suite.ip;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;

import suite.Suite;
import suite.asm.Assembler;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.primitive.Bytes;

public class ImperativeCompilerTest {

	@Test
	public void test() {
		Bytes bytes = compile("`0` = 1;");
		assertNotNull(bytes);
		System.out.println(bytes);
	}

	private Bytes compile(String ip) {
		RuleSet ruleSet = Suite.createRuleSet(Arrays.asList("asm.sl", "auto.sl", "ic.sl"));
		Finder finder = new InterpretedProverBuilder().build(ruleSet, Suite.parse("" //
				+ "source .ip" //
				+ ", ic-compile 0 .ip .code/()" //
				+ ", sink .code"));

		Node code = FindUtil.collectSingle(finder, Suite.parse(ip));
		return new Assembler(32).assemble(Suite.substitute(".0, .1", Suite.parse(".org = 0"), code));
	}

}
