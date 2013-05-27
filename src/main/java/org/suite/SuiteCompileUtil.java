package org.suite;

import java.util.Arrays;
import java.util.List;

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
			logicalCompiler = createRuleSet(Arrays.asList("auto.sl", "lc.sl"));
		return logicalCompiler;
	}

	public synchronized RuleSet eagerFunRuleSet() {
		if (eagerFunCompiler == null)
			eagerFunCompiler = createRuleSet(Arrays.asList( //
					"auto.sl", "fc.sl", "fc-eager-evaluation.sl"));
		return eagerFunCompiler;
	}

	public synchronized RuleSet lazyFunRuleSet() {
		if (lazyFunCompiler == null)
			lazyFunCompiler = createRuleSet(Arrays.asList( //
					"auto.sl", "fc.sl", "fc-lazy-evaluation.sl"));
		return lazyFunCompiler;
	}

	public void precompile(String libraryName, ProverConfig proverConfig) {
		System.out.println("Pre-compiling " + libraryName + "... ");
		List<String> imports = Arrays.asList("auto.sl", "fc-precompile.sl");

		RuleSet rs = createRuleSet(imports);
		Prover prover = new Prover(new ProverConfig(rs, proverConfig));

		String goal = "fc-setup-precompile " + libraryName;
		Node node = Suite.parse(goal);

		if (prover.prove(node))
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed");
	}

	private RuleSet createRuleSet(List<String> toImports) {
		return Suite.createRuleSet(toImports);
	}

}
