package suite.ip;

import suite.Suite;
import suite.asm.Assembler;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.primitive.Bytes;

public class ImperativeCompiler {

	private RuleSet ruleSet = Suite.imperativeCompilerRuleSet();
	private Finder finder = new InterpretedProverBuilder().build(ruleSet, Suite.parse("" //
			+ "source .ip" //
			+ ", ic-compile 0 .ip .code/()" //
			+ ", sink .code"));

	public Bytes compile(int org, String ip) {
		Node code = FindUtil.collectSingle(finder, Suite.parse(ip));
		return new Assembler(32).assemble(Suite.substitute(".0, .1", Suite.parse(".org = " + org), code));
	}

}
