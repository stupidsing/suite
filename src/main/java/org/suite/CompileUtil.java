package org.suite;

import java.util.Arrays;
import java.util.List;

import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;

public class CompileUtil {

	private RuleSet logicalRuleSet;
	private RuleSet funRuleSet;
	private RuleSet eagerFunRuleSet;
	private RuleSet lazyFunRuleSet;

	public synchronized RuleSet logicCompilerRuleSet() {
		if (logicalRuleSet == null)
			logicalRuleSet = createRuleSet(Arrays.asList("auto.sl", "lc.sl"));
		return logicalRuleSet;
	}

	public synchronized RuleSet funCompilerRuleSet() {
		if (funRuleSet == null)
			funRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl"));
		return funRuleSet;
	}

	public RuleSet funCompilerRuleSet(boolean isLazy) {
		return isLazy ? lazyFunCompilerRuleSet() : eagerFunCompilerRuleSet();
	}

	public boolean precompile(String libraryName, ProverConfig pc) {
		System.out.println("Pre-compiling " + libraryName + "... ");

		RuleSet rs = createRuleSet(Arrays.asList("auto.sl", "fc-precompile.sl"));
		Prover prover = new Prover(new ProverConfig(rs, pc));
		Node node = Suite.parse("fc-setup-precompile " + libraryName);
		boolean result = prover.prove(node);

		if (result)
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed\n");

		return result;
	}

	private synchronized RuleSet eagerFunCompilerRuleSet() {
		if (eagerFunRuleSet == null)
			eagerFunRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl", "fc-eager-evaluation.sl"));
		return eagerFunRuleSet;
	}

	private synchronized RuleSet lazyFunCompilerRuleSet() {
		if (lazyFunRuleSet == null)
			lazyFunRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl", "fc-lazy-evaluation.sl"));
		return lazyFunRuleSet;
	}

	private RuleSet createRuleSet(List<String> toImports) {
		return Suite.createRuleSet(toImports);
	}

}
