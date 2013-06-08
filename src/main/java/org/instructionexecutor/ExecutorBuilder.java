package org.instructionexecutor;

import org.suite.doer.Prover;
import org.suite.node.Node;

public class ExecutorBuilder {

	public interface Builder {
		public Executor build(Node code);
	}

	public interface Executor {
		public void execute();
	}

	public class InterpretedFunExecutorBuilder implements Builder {
		public Executor build(final Node code) {
			return new Executor() {
				public void execute() {
					try (InstructionExecutor executor = new FunInstructionExecutor(code)) {
						executor.execute();
					}
				}
			};
		}
	}

	public class InterpretedLogicExecutorBuilder implements Builder {
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
