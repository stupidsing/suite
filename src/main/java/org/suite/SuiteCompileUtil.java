package org.suite;

import java.util.Arrays;
import java.util.List;

import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;

public class SuiteCompileUtil {

	private RuleSet logicalRuleSet;
	private RuleSet funRuleSet;
	private RuleSet eagerFunRuleSet;
	private RuleSet lazyFunRuleSet;

	public synchronized RuleSet logicalRuleSet() {
		if (logicalRuleSet == null)
			logicalRuleSet = createRuleSet(Arrays.asList("auto.sl", "lc.sl"));
		return logicalRuleSet;
	}

	public synchronized RuleSet funRuleSet() {
		if (funRuleSet == null)
			funRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl"));
		return funRuleSet;
	}

	public synchronized RuleSet eagerFunRuleSet() {
		if (eagerFunRuleSet == null)
			eagerFunRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl", "fc-eager-evaluation.sl"));
		return eagerFunRuleSet;
	}

	public synchronized RuleSet lazyFunRuleSet() {
		if (lazyFunRuleSet == null)
			lazyFunRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl", "fc-lazy-evaluation.sl"));
		return lazyFunRuleSet;
	}

	public void precompile(String libraryName, ProverConfig proverConfig) {
		System.out.println("Pre-compiling " + libraryName + "... ");

		RuleSet rs = createRuleSet(Arrays.asList("auto.sl", "fc-precompile.sl"));
		Prover prover = new Prover(new ProverConfig(rs, proverConfig));
		Node node = Suite.parse("fc-setup-precompile " + libraryName);

		if (prover.prove(node))
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed");
	}

	private RuleSet createRuleSet(List<String> toImports) {
		return Suite.createRuleSet(toImports);
	}

}
