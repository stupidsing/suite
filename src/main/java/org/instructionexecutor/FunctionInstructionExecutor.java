package org.instructionexecutor;

import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;

public class FunctionInstructionExecutor extends InstructionExecutor {

	public FunctionInstructionExecutor(Node node) {
		super(node);
	}

	@Override
	protected int[] execute(Closure current, Instruction insn,
			Closure callStack[], int csp, Object dataStack[], int dsp) {
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		switch (insn.insn) {
		case SYS___________:
			dsp -= insn.op3;
			regs[insn.op2] = sys(constantPool.get(insn.op1), dataStack, dsp);
			break;
		default:
			return super.execute(current, insn, callStack, csp, dataStack, dsp);
		}

		return new int[] { csp, dsp };
	}

	private Node sys(Node command, Object dataStack[], int dsp) {
		Node result;

		if (command == Atom.create("CONS")) {
			Node left = (Node) dataStack[dsp + 1];
			Node right = (Node) dataStack[dsp];
			result = new Tree(TermOp.COLON_, left, right);
		} else if (command == Atom.create("EMPTY"))
			result = Atom.nil;
		else if (command == Atom.create("IS-TREE"))
			result = a(Tree.decompose((Node) dataStack[dsp]) != null);
		else if (command == Atom.create("HEAD"))
			result = Tree.decompose((Node) dataStack[dsp]).getLeft();
		else if (command == Atom.create("TAIL"))
			result = Tree.decompose((Node) dataStack[dsp]).getRight();
		else
			throw new RuntimeException("Unknown system call " + command);

		return result;
	}

}
