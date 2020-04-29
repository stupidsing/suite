package suite.ip;

import java.nio.file.Path;
import java.util.List;

import primal.Verbs.ReadString;
import primal.primitive.adt.Bytes;
import suite.Suite;
import suite.asm.StackAssembler;
import suite.assembler.Amd64Mode;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.parser.IncludePreprocessor;
import suite.text.Preprocess;

public class ImperativeCompiler {

	private RuleSet ruleSet = Suite.imperativeCompilerRuleSet();

	private Finder finder = new SewingProverBuilder2()
			.build(ruleSet)
			.apply(Suite.parse(""
					+ "source .ip"
					+ ", compile-imperative .ip .code/()"
					+ ", sink .code"));

	public Bytes compile(int org, Path path) {
		var s0 = ReadString.from(path);
		var s1 = Preprocess.transform(List.of(new IncludePreprocessor(path.getParent())::preprocess), s0).k;
		return compile(org, s1);
	}

	public Bytes compile(int org, String ip) {
		var code = finder.collectSingle(Suite.parse(ip));
		var sa = new StackAssembler(Amd64Mode.PROT32);
		return sa.assembler.assemble(Suite.substitute(".0, .1", Suite.parse(".org = " + org), code));
	}

}
