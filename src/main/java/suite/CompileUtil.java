package suite;

import java.util.List;

import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.SewingProverBuilder;
import suite.util.FunUtil.Fun;
import suite.util.Memoize;

public class CompileUtil {

	private Fun<List<String>, RuleSet> newRuleSetFun = Memoize.fun(Suite::newRuleSet);

	public synchronized RuleSet funCompilerRuleSet() {
		return newRuleSetFun.apply(List.of("auto.sl", "fc/fc.sl"));
	}

	public synchronized RuleSet imperativeCompilerRuleSet() {
		return newRuleSetFun.apply(List.of("asm.sl", "auto.sl", "ic/ic.sl"));
	}

	public synchronized RuleSet logicCompilerRuleSet() {
		return newRuleSetFun.apply(List.of("auto.sl", "lc/lc.sl"));
	}

	public boolean precompile(String libraryName, ProverConfig pc) {
		System.out.println("Pre-compiling [" + libraryName + "]...");

		Builder builder = new SewingProverBuilder(pc);
		RuleSet rs = funCompilerRuleSet();
		boolean result = Suite.proveLogic(builder, rs, "fc-precompile-lib " + libraryName);

		if (result)
			System.out.println("Pre-compilation success [" + libraryName + "]");
		else
			System.out.println("Pre-compilation failed [" + libraryName + "]");

		return result;
	}

}
