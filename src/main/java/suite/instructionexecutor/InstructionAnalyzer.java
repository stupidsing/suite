package suite.instructionexecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import primal.fp.Funs.Source;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.instructionexecutor.InstructionUtil.Thunk;
import suite.node.Node;
import suite.node.Tree;

public class InstructionAnalyzer {

	private List<AnalyzedFrame> frameByIp = new ArrayList<>();
	private Set<Integer> tailCalls = new HashSet<>();

	public static class AnalyzedFrame {
		private int frameBeginIp;
		private boolean isRequireParent = false;
		private AnalyzedFrame parent;
		private List<AnalyzedRegister> registers;

		public AnalyzedFrame(int ip) {
			frameBeginIp = ip;
		}

		public boolean isAccessedByChildFrames() {
			return registers.stream().anyMatch(AnalyzedRegister::isAccessedByChildFrames);
		}

		public int getFrameBeginIp() {
			return frameBeginIp;
		}

		public boolean isRequireParent() {
			return isRequireParent;
		}

		public AnalyzedFrame getParent() {
			return parent;
		}

		public List<AnalyzedRegister> getRegisters() {
			return registers;
		}
	}

	public static class AnalyzedRegister {
		private Class<?> clazz;
		private boolean isAccessedByChildFrames = false;

		public Class<?> getClazz() {
			return clazz;
		}

		/**
		 * Analyzes whether code of other frames would access this variable.
		 */
		public boolean isAccessedByChildFrames() {
			return isAccessedByChildFrames;
		}

		/**
		 * Analyzes whether the variable can be stored in a local variable, instead of a
		 * instance variable in a frame.
		 */
		public boolean isTemporal() {
			return false;
		}
	}

	public void analyze(List<Instruction> instructions) {

		// identify frame regions
		analyzeFrames(instructions);

		// discover frame hierarchy
		analyzeParentFrames(instructions);

		// find out register types in each frame
		analyzeFrameRegisters(instructions);

		// find out tail call sites possible for optimization
		analyzeFpTailCalls(instructions);
	}

	private void analyzeFrames(List<Instruction> instructions) {
		var analyzedFrames = new ArrayDeque<AnalyzedFrame>();

		// find out the parent of closures.
		// assumes every FRAME-BEGIN has a ASSIGN-THUNK referencing it.
		for (var ip = 0; ip < instructions.size(); ip++) {
			var insn = instructions.get(ip);

			if (insn.insn == Insn.FRAMEBEGIN____)
				analyzedFrames.push(new AnalyzedFrame(ip));

			AnalyzedFrame frame = !analyzedFrames.isEmpty() ? analyzedFrames.peek() : null;
			frameByIp.add(frame);

			if (insn.insn == Insn.FRAMEEND______)
				analyzedFrames.pop();
		}
	}

	private void analyzeParentFrames(List<Instruction> instructions) {
		for (var ip = 0; ip < instructions.size(); ip++) {
			var insn = instructions.get(ip);

			// recognize frames and their parents.
			// assume ASSIGN-THUNK points to the FRAME-BEGIN instruction.
			if (insn.insn == Insn.ASSIGNTHUNK___)
				frameByIp.get(insn.op1).parent = frameByIp.get(ip);
		}
	}

	private void analyzeFrameRegisters(List<Instruction> instructions) {
		var ip = 0;

		while (ip < instructions.size()) {
			var currentIp = ip;
			var insn = instructions.get(ip++);
			int op0 = insn.op0, op1 = insn.op1, op2 = insn.op2;
			var frame = frameByIp.get(currentIp);
			List<AnalyzedRegister> registers = frame != null ? frame.registers : null;

			switch (insn.insn) {
			case EVALEQ________:
			case EVALLE________:
			case EVALLT________:
			case EVALNE________:
			case ISCONS________:
				registers.get(op0).clazz = boolean.class;
				break;
			case ASSIGNTHUNK___:
				registers.get(op0).clazz = Thunk.class;
				break;
			case ASSIGNINT_____:
			case BACKUPCSP_____:
			case BACKUPDSP_____:
			case BINDMARK______:
			case COMPARE_______:
			case ERROR_________:
			case EVALADD_______:
			case EVALDIV_______:
			case EVALMOD_______:
			case EVALMUL_______:
			case EVALSUB_______:
				registers.get(op0).clazz = int.class;
				break;
			case ASSIGNCONST___:
			case ASSIGNRESULT__:
			case ASSIGNTHUNKRES:
			case CALLINTRINSIC_:
			case CONSPAIR______:
			case CONSLIST______:
			case DATACHARS_____:
			case GETINTRINSIC__:
			case HEAD__________:
			case IFNOTCONS_____:
			case IFNOTPAIR_____:
			case LOGREG________:
			case NEWNODE_______:
			case POP___________:
			case TAIL__________:
			case TOP___________:
				registers.get(op0).clazz = Node.class;
				break;
			case FORMTREE1_____:
				registers.get(insn.op1).clazz = Tree.class;
				break;
			case ASSIGNFRAMEREG:
				var frame1 = frame;
				for (var i = op1; i < 0; i++) {
					frame1.isRequireParent = true;
					frame1 = frame1.parent;
				}

				var op0register = registers.get(op0);
				var op2Register = frame1.registers.get(op2);

				if (frame != frame1)
					op2Register.isAccessedByChildFrames = true;

				// merge into Node if clashed
				if (op0register.clazz != op2Register.clazz)
					op0register.clazz = op0register.clazz != null ? Node.class : op2Register.clazz;
				break;
			case DECOMPOSETREE1:
				registers.get(op1).clazz = registers.get(op2).clazz = Node.class;
				break;
			case FRAMEBEGIN____:
				registers = frame.registers = new ArrayList<>();
				for (var i = 0; i < op0; i++)
					registers.add(new AnalyzedRegister());
				break;
			default:
			}
		}
	}

	private void analyzeFpTailCalls(List<Instruction> instructions) {
		for (var ip = 0; ip < instructions.size() - 1; ip++) {
			Source<Instruction> source = flow(instructions, ip);
			var instruction0 = source.g();
			var instruction1 = source.g();

			if (instruction0 != null && instruction0.insn == Insn.CALLTHUNK_____ //
					&& instruction1 != null && instruction1.insn == Insn.ASSIGNRESULT__ //
					&& isReturningValue(source, instruction1.op0))
				tailCalls.add(ip);
		}
	}

	private boolean isReturningValue(Source<Instruction> source, int returnReg) {
		Instruction instruction;
		boolean isLeft = false, isReturningValue = false;

		while ((instruction = source.g()) != null)
			switch (instruction.insn) {
			case ASSIGNFRAMEREG -> {
				if (instruction.op1 == 0 && instruction.op2 == returnReg) {
					returnReg = instruction.op0;
				} else
					return false;
			}
			case LEAVE_________ -> {
				isLeft = true;
			}
			case RETURN________ -> {
				return isLeft && isReturningValue;
			}
			case SETRESULT_____ -> {
				isReturningValue = instruction.op0 == returnReg;
			}
			default -> {
				return false;
			}
			}

		return false;
	}

	private Source<Instruction> flow(List<Instruction> instructions, int ip) {
		return new Source<>() {
			private boolean end = false;
			private int ip_ = ip;

			public Instruction g() {
				if (!end && ip_ < instructions.size()) {
					var instruction = instructions.get(ip_++);

					switch (instruction.insn) {
					case ASSIGNFRAMEREG:
					case ASSIGNRESULT__:
					case ASSIGNTHUNKRES:
					case CALL__________:
					case CALLINTRINSIC_:
					case CALLTHUNK_____:
					case LEAVE_________:
					case SETRESULT_____:
						return instruction;
					case JUMP__________:
						ip_ = instruction.op0;
					case REMARK________:
						return g();
					default:
						end = true;
						return instruction;
					}
				} else
					return null;
			}
		};
	}

	public void transform(List<Instruction> instructions) {
		for (var ip : tailCalls)
			instructions.get(ip).insn = Insn.JUMPCLOSURE___;
	}

	public AnalyzedFrame getFrame(Integer ip) {
		return frameByIp.get(ip);
	}

}
