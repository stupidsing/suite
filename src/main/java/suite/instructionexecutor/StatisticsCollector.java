package suite.instructionexecutor;

import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.os.LogUtil;

public class StatisticsCollector {

	private static StatisticsCollector instance = new StatisticsCollector();

	private int nInstructionsExecuted;

	private StatisticsCollector() {
	}

	public static StatisticsCollector getInstance() {
		return instance;
	}

	public void collect(int ip, Instruction insn) {
		LogUtil.info(ip + "> " + insn);
		nInstructionsExecuted++;

		if (insn.insn == Insn.EXIT__________) { // ends collection
			log();
			instance = new StatisticsCollector();
		}
	}

	private void log() {
		LogUtil.info("nInstructionsExecuted = " + nInstructionsExecuted);
	}

}
