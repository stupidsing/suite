package org.suite;

import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;

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

	public void precompile(String libraryName, ProverConfig proverConfig) {
		System.out.println("Pre-compiling " + libraryName + "... ");
		String imports[] = { "auto.sl", "fc-precompile.sl" };

		RuleSet rs = createRuleSet(imports);
		Prover prover = new Prover(new ProverConfig(rs, proverConfig));

		String goal = "fc-setup-precompile " + libraryName;
		Node node = Suite.parse(goal);

		if (prover.prove(node))
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed");
	}

	private RuleSet createRuleSet(String toImports[]) {
		return Suite.createRuleSet(toImports);
	}

}
