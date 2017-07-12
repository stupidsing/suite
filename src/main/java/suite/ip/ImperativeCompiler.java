package suite.ip;

import java.nio.file.Path;
import java.util.Arrays;

import suite.Suite;
import suite.asm.StackAssembler;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.node.Node;
import suite.parser.IncludePreprocessor;
import suite.primitive.Bytes;
import suite.text.Preprocess;
import suite.util.To;

public class ImperativeCompiler {

	private RuleSet ruleSet = Suite.imperativeCompilerRuleSet();
	private Finder finder = new SewingProverBuilder2() //
			.build(ruleSet) //
			.apply(Suite.parse("" //
					+ "source .ip" //
					+ ", compile-imperative .ip .code/()" //
					+ ", sink .code"));

	public Bytes compile(int org, Path path) {
		String s0 = To.string(path);
		String s1 = Preprocess.transform(Arrays.asList(new IncludePreprocessor(path.getParent())), s0).t0;
		return compile(org, s1);
	}

	public Bytes compile(int org, String ip) {
		Node code = FindUtil.collectSingle(finder, Suite.parse(ip));
		return new StackAssembler(32).assembler.assemble(Suite.substitute(".0, .1", Suite.parse(".org = " + org), code));
	}

}
