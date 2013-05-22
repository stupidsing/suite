package org.suite;

import org.suite.kb.RuleSet;

public class SuiteCompileUtil {

	private RuleSet logicalCompiler;
	private RuleSet eagerFunCompiler;
	private RuleSet lazyFunCompiler;

	public synchronized RuleSet logicalRuleSet() {
		if (logicalCompiler == null)
			logicalCompiler = createRuleSet(new String[] { "auto.sl", "lc.sl" });
		return logicalCompiler;
	}

	public synchronized RuleSet eagerFunRuleSet() {
		if (eagerFunCompiler == null) {
			String imports[] = { "auto.sl", "fc.sl", "fc-eager-evaluation.sl" };
			eagerFunCompiler = createRuleSet(imports);
		}
		return eagerFunCompiler;
	}

	public synchronized RuleSet lazyFunRuleSet() {
		if (lazyFunCompiler == null) {
			String imports[] = { "auto.sl", "fc.sl", "fc-lazy-evaluation.sl" };
			lazyFunCompiler = createRuleSet(imports);
		}
		return lazyFunCompiler;
	}

	private RuleSet createRuleSet(String toImports[]) {
		return SuiteUtil.createRuleSet(toImports);
	}

}
