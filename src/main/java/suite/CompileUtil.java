package suite;

import java.util.Arrays;
import java.util.List;

import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.CompositeRuleSet;
import suite.lp.kb.RuleSet;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.util.FunUtil.Fun;
import suite.util.Memoize;

public class CompileUtil {

	private Fun<List<String>, RuleSet> createRuleSetFun = Memoize.byInput(Suite::createRuleSet);

	/**
	 * Returns rule set for functional compiler.
	 *
	 * The functional compiler would perform asserts when libraries are used.
	 *
	 * Use composite rule set to store new rules, and avoid original rule set
	 * being altered.
	 */
	public synchronized RuleSet funCompilerRuleSet() {
		return new CompositeRuleSet(createRuleSetFun.apply(Arrays.asList("auto.sl", "fc.sl")));
	}

	public synchronized RuleSet imperativeCompilerRuleSet() {
		return createRuleSetFun.apply(Arrays.asList("asm.sl", "auto.sl", "ic.sl"));
	}

	public synchronized RuleSet logicCompilerRuleSet() {
		return createRuleSetFun.apply(Arrays.asList("auto.sl", "lc.sl"));
	}

	public boolean precompile(String libraryName, ProverConfig pc) {
		System.out.println("Pre-compiling [" + libraryName + "]...");

		RuleSet rs = createRuleSetFun.apply(Arrays.asList("auto.sl", "fc-precompile.sl"));
		Builder builder = new InterpretedProverBuilder(pc);
		boolean result = Suite.proveLogic(builder, rs, "fc-precompile-lib " + libraryName);

		if (result)
			System.out.println("Pre-compilation success [" + libraryName + "]");
		else
			System.out.println("Pre-compilation failed [" + libraryName + "]");

		return result;
	}

}
