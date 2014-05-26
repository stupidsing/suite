package suite.lp.search;

import suite.Suite;
import suite.instructionexecutor.InstructionExecutor;
import suite.instructionexecutor.LogicInstructionExecutor;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.util.LogUtil;

public class CompiledProverBuilder implements Builder {

	private ProverConfig proverConfig;
	private Finder compiler;

	/**
	 * Creates a builder that interpretes the logic compiler to compile the
	 * given code, then execute.
	 */
	public static CompiledProverBuilder level1(ProverConfig proverConfig, boolean isDumpCode) {
		return new CompiledProverBuilder(new InterpretedProverBuilder(proverConfig), proverConfig, isDumpCode);
	}

	/**
	 * Creates a builder that compiles the logic compiler, execute it to compile
	 * the given code, then execute.
	 */
	public static CompiledProverBuilder level2(ProverConfig proverConfig, boolean isDumpCode) {
		return new CompiledProverBuilder(level1(proverConfig, false), proverConfig, isDumpCode);
	}

	private CompiledProverBuilder(Builder builder, ProverConfig proverConfig, boolean isDumpCode) {
		this.compiler = createCompiler(builder, isDumpCode);
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(RuleSet ruleSet, Node goal) {
		Node goal1 = Suite.substitute(".0 >> .1", Suite.ruleSetToNode(ruleSet), goal);
		Node code = compile(goal1);
		ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);

		return (source, sink) -> {
			proverConfig1.setSource(source);
			proverConfig1.setSink(sink);

			try (InstructionExecutor executor = new LogicInstructionExecutor(code, proverConfig1)) {
				executor.execute();
			}
		};
	}

	private Node compile(Node program) {
		return LogUtil.duration("Code compiled", () -> FindUtil.collectSingle(compiler, program));
	}

	private Finder createCompiler(Builder builder, boolean isDumpCode) {
		String compile = "source .in, compile-logic .in .out";
		compile += isDumpCode ? ", pretty.print .out" : "";
		compile += ", sink .out";
		return builder.build(Suite.logicCompilerRuleSet(), Suite.parse(compile));
	}

}
