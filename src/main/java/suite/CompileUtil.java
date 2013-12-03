package suite;

import java.util.Arrays;
import java.util.List;

import suite.lp.doer.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.util.CacheUtil;
import suite.util.FunUtil.Fun;

public class CompileUtil {

	private Fun<List<String>, RuleSet> createRuleSetFun = new CacheUtil().proxy(new Fun<List<String>, RuleSet>() {
		public RuleSet apply(List<String> filenames) {
			return Suite.createRuleSet(filenames);
		}
	});

	public synchronized RuleSet logicCompilerRuleSet() {
		return createRuleSetFun.apply(Arrays.asList("auto.sl", "lc.sl"));
	}

	public synchronized RuleSet funCompilerRuleSet() {
		return createRuleSetFun.apply(Arrays.asList("auto.sl", "fc.sl"));
	}

	public boolean precompile(String libraryName, ProverConfig pc) {
		System.out.println("Pre-compiling " + libraryName + "...");

		RuleSet rs = createRuleSetFun.apply(Arrays.asList("auto.sl", "fc-precompile.sl"));
		Builder builder = new InterpretedProverBuilder(pc);
		boolean result = Suite.proveLogic(builder, rs, "fc-setup-precompile " + libraryName);

		if (result)
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed\n");

		return result;
	}

}
