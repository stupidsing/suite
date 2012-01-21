package org.instructionexecutor;

import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;

public class FunctionInstructionExecutor extends InstructionExecutor {

	private static final Atom CONS = Atom.create("CONS");
	private static final Atom EMPTY = Atom.create("EMPTY");
	private static final Atom HEAD = Atom.create("HEAD");
	private static final Atom ISTREE = Atom.create("IS-TREE");
	private static final Atom LOG = Atom.create("LOG");
	private static final Atom TAIL = Atom.create("TAIL");

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

		if (command == CONS) {
			Node left = (Node) dataStack[dsp + 1];
			Node right = (Node) dataStack[dsp];
			result = new Tree(TermOp.COLON_, left, right);
		} else if (command == EMPTY)
			result = Atom.nil;
		else if (command == HEAD)
			result = Tree.decompose((Node) dataStack[dsp]).getLeft();
		else if (command == ISTREE)
			result = a(Tree.decompose((Node) dataStack[dsp]) != null);
		else if (command == LOG) {
			System.out.println((Node) dataStack[dsp + 1]);
			result = (Node) dataStack[dsp];
		} else if (command == TAIL)
			result = Tree.decompose((Node) dataStack[dsp]).getRight();
		else
			throw new RuntimeException("Unknown system call " + command);

		return result;
	}

}
