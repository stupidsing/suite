package org.instructionexecutor;

import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.node.Node;

/**
 * Input/output (executeIo)?
 *
 * Unwrapping in lazy functional mode?
 */
public class ExecutorBuilder {

	public interface Builder {
		public Executor build(Node code);
	}

	public interface Executor {
		public void execute();
	}

	public static class InterpretedFunExecutorBuilder implements Builder {
		private ProverConfig proverConfig;

		public InterpretedFunExecutorBuilder(ProverConfig proverConfig) {
			this.proverConfig = proverConfig;
		}

		public Executor build(final Node code) {
			return new Executor() {
				public void execute() {
					try (FunInstructionExecutor executor = new FunInstructionExecutor(code)) {
						executor.setProverConfig(proverConfig);
						executor.execute();
					}
				}
			};
		}
	}

	public static class InterpretedLogicExecutorBuilder implements Builder {
		private Prover prover;

		public InterpretedLogicExecutorBuilder(Prover prover) {
			this.prover = prover;
		}

		public Executor build(final Node code) {
			return new Executor() {
				public void execute() {
					try (InstructionExecutor executor = new LogicInstructionExecutor(code, prover)) {
						executor.execute();
					}
				}
			};
		}
	}

}
