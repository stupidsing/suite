package suite;

import java.util.List;

import suite.lp.Configuration.ProverCfg;
import suite.lp.kb.RuleSet;
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

	public boolean precompile(String libraryName, ProverCfg pc) {
		System.out.println("Pre-compiling [" + libraryName + "]...");

		var builder = new SewingProverBuilder(pc);
		var rs = funCompilerRuleSet();
		var b = Suite.proveLogic(builder, rs, "fc-precompile-lib " + libraryName);

		System.out.println("Pre-compilation " + (b ? "success" : "failed") + " [" + libraryName + "]");

		return b;
	}

}
