package suite.lp.search;

import suite.Suite;
import suite.instructionexecutor.LogicInstructionExecutor;
import suite.lp.Configuration.ProverCfg;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.os.LogUtil;
import suite.streamlet.FunUtil.Fun;

public class CompiledProverBuilder implements Builder {

	private ProverCfg proverCfg;
	private Finder compiler;

	/**
	 * Creates a builder that interpretes the logic compiler to compile the
	 * given code, then execute.
	 */
	public static CompiledProverBuilder level1(ProverCfg proverCfg) {
		return new CompiledProverBuilder(new SewingProverBuilder2(proverCfg), proverCfg);
	}

	/**
	 * Creates a builder that compiles the logic compiler, execute it to compile
	 * the given code, then execute.
	 */
	public static CompiledProverBuilder level2(ProverCfg proverCfg) {
		return new CompiledProverBuilder(level1(proverCfg), proverCfg);
	}

	private CompiledProverBuilder(Builder builder, ProverCfg proverCfg) {
		this.compiler = newCompiler(builder);
		this.proverCfg = proverCfg;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		var rules = Suite.getRules(ruleSet);

		return goal -> {
			var code = compile(Suite.substitute(".0 ~ .1", rules, goal));

			return (source, sink) -> {
				var proverCfg1 = new ProverCfg(ruleSet, proverCfg);
				proverCfg1.setSource(source);
				proverCfg1.setSink(sink);

				try (var executor = new LogicInstructionExecutor(code, proverCfg1)) {
					executor.execute();
				}
			};
		};
	}

	private Node compile(Node program) {
		return LogUtil.duration("Code compiled", () -> compiler.collectSingle(program));
	}

	private Finder newCompiler(Builder builder) {
		var compile = "source .in, compile-logic .in .out, sink .out";
		return builder.build(Suite.logicCompilerRuleSet()).apply(Suite.parse(compile));
	}

}
