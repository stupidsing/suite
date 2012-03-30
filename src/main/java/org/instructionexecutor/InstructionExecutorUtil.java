package org.instructionexecutor;

import org.instructionexecutor.InstructionExecutor.Insn;
import org.suite.node.Node;

public class InstructionExecutorUtil {

	protected static class Instruction {
		protected Insn insn;
		protected int op1, op2, op3;

		protected Instruction(Insn insn, int op1, int op2, int op3) {
			this.insn = insn;
			this.op1 = op1;
			this.op2 = op2;
			this.op3 = op3;
		}

		public String toString() {
			return insn.name + " " + op1 + ", " + op2 + ", " + op3;
		}
	}

	// Indicates a function call with a specified set of framed environment.
	// Closure must extend Node in order to be put in a list (being cons-ed).
	protected static class Closure extends Node {
		protected Frame frame;
		protected int ip;

		protected Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}

		protected Closure clone() {
			return new Closure(frame, ip);
		}

		public String toString() {
			return "frameSize = " + frame.registers.length + ", IP = " + ip;
		}
	}

	protected static class Frame {
		protected Frame previous;
		protected Node registers[];

		protected Frame(Frame previous, int frameSize) {
			this.previous = previous;
			registers = new Node[frameSize];
		}
	}

	protected static class CutPoint {
		protected int journalPointer;
		protected int callStackPointer;

		protected CutPoint(int journalPointer, int callStackPointer) {
			this.journalPointer = journalPointer;
			this.callStackPointer = callStackPointer;
		}
	}

}
