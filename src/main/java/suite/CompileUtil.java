package suite;

import java.util.Arrays;
import java.util.List;

import suite.lp.doer.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;

public class CompileUtil {

	private RuleSet logicalCompilerRuleSet;
	private RuleSet funCompilerRuleSet;

	public synchronized RuleSet logicCompilerRuleSet() {
		if (logicalCompilerRuleSet == null)
			logicalCompilerRuleSet = createRuleSet(Arrays.asList("auto.sl", "lc.sl"));
		return logicalCompilerRuleSet;
	}

	public synchronized RuleSet funCompilerRuleSet() {
		if (funCompilerRuleSet == null)
			funCompilerRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl"));
		return funCompilerRuleSet;
	}

	public boolean precompile(String libraryName, ProverConfig pc) {
		System.out.println("Pre-compiling " + libraryName + "... ");

		RuleSet rs = createRuleSet(Arrays.asList("auto.sl", "fc-precompile.sl"));
		Builder builder = new InterpretedProverBuilder(pc);
		boolean result = Suite.proveLogic(builder, rs, "fc-setup-precompile " + libraryName);

		if (result)
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed\n");

		return result;
	}

	private RuleSet createRuleSet(List<String> toImports) {
		return Suite.createRuleSet(toImports);
	}

}
