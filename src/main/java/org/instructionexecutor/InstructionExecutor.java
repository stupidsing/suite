package org.instructionexecutor;

import java.util.ArrayList;
import java.util.List;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Insn;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.suite.doer.Comparer;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.util.LogUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionExecutor {

	private static final int stackSize = 4096;

	private Instruction instructions[];
	private int unwrapEntryPoint;

	protected BiMap<Integer, Node> constantPool = HashBiMap.create();

	public InstructionExecutor(Node node) {
		InstructionExtractor extractor = new InstructionExtractor(constantPool);

		List<Instruction> list = new ArrayList<>();
		list.addAll(extractor.extractInstructions(node));
		unwrapEntryPoint = list.size();
		list.add(new Instruction(Insn.CALLCLOSURE___, 1, 0, 0));
		list.add(new Instruction(Insn.EXIT__________, 1, 0, 0));

		instructions = list.toArray(new Instruction[list.size()]);
	}

	public Node execute() {
		return evaluateClosure(new Closure(null, 0));
	}

	public Node evaluateClosure(Closure c0) {
		Frame f0 = new Frame(null, 2);
		f0.registers[0] = c0;

		Activation current = new Activation(f0, unwrapEntryPoint, null);

		Node stack[] = new Node[stackSize];
		int i, sp = 0;

		Exec exec = new Exec();
		exec.stack = stack;

		Comparer comparer = new Comparer();

		for (;;) {
			Frame frame = current.frame;
			Node regs[] = frame != null ? frame.registers : null;
			int ip = current.ip++;
			Instruction insn = instructions[ip];
			TermOp op;

			// org.util.LogUtil.info(ip + "> " + insn);

			switch (insn.insn) {
			case ASSIGNCLOSURE_:
				regs[insn.op0] = new Closure(frame, insn.op1);
				break;
			case ASSIGNFRAMEREG:
				i = insn.op1;
				while (i++ < 0)
					frame = frame.previous;
				regs[insn.op0] = frame.registers[insn.op2];
				break;
			case ASSIGNCONST___:
				regs[insn.op0] = constantPool.get(insn.op1);
				break;
			case ASSIGNINT_____:
				regs[insn.op0] = number(insn.op1);
				break;
			case CALL__________:
				current = new Activation(frame, i(regs[insn.op0]), current);
				break;
			case CALLCONST_____:
				current = new Activation(frame, insn.op0, current);
				break;
			case CALLCLOSURE___:
				Closure closure = (Closure) regs[insn.op1];
				if (closure.result == null)
					current = new Activation(closure, current);
				else
					regs[insn.op0] = closure.result;
				break;
			case DECOMPOSETREE0:
				Node node = regs[insn.op0];
				op = TermOp.find(((Atom) constantPool.get(insn.op1)).getName());
				int branch = insn.op2;
				insn = instructions[current.ip++];
				Tree tree = Tree.decompose(node, op);
				if (tree != null) {
					regs[insn.op0] = tree.getLeft();
					regs[insn.op1] = tree.getRight();
				} else
					current.ip = branch;
				break;
			case ENTER_________:
				current.frame = new Frame(frame, insn.op0);
				break;
			case EVALADD_______:
				regs[insn.op0] = number(i(regs[insn.op1]) + i(regs[insn.op2]));
				break;
			case EVALDIV_______:
				regs[insn.op0] = number(i(regs[insn.op1]) / i(regs[insn.op2]));
				break;
			case EVALEQ________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i == 0);
				break;
			case EVALGE________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i >= 0);
				break;
			case EVALGT________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i > 0);
				break;
			case EVALLE________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i <= 0);
				break;
			case EVALLT________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i < 0);
				break;
			case EVALNE________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i != 0);
				break;
			case EVALMOD_______:
				regs[insn.op0] = number(i(regs[insn.op1]) % i(regs[insn.op2]));
				break;
			case EVALMUL_______:
				regs[insn.op0] = number(i(regs[insn.op1]) * i(regs[insn.op2]));
				break;
			case EVALSUB_______:
				regs[insn.op0] = number(i(regs[insn.op1]) - i(regs[insn.op2]));
				break;
			case EXIT__________:
				return regs[insn.op0];
			case FORMTREE0_____:
				Node left = regs[insn.op0];
				Node right = regs[insn.op1];
				insn = instructions[current.ip++];
				op = TermOp.find(((Atom) constantPool.get(insn.op0)).getName());
				regs[insn.op1] = Tree.create(op, left, right);
				break;
			case IFFALSE_______:
				if (regs[insn.op1] != Atom.TRUE)
					current.ip = insn.op0;
				break;
			case IFNOTEQUALS___:
				if (regs[insn.op1] != regs[insn.op2])
					current.ip = insn.op0;
				break;
			case JUMP__________:
				current.ip = insn.op0;
				break;
			case LABEL_________:
				break;
			case LOG___________:
				LogUtil.info(constantPool.get(insn.op0).toString());
				break;
			case NEWNODE_______:
				regs[insn.op0] = new Reference();
				break;
			case PUSH__________:
				stack[sp++] = regs[insn.op0];
				break;
			case PUSHCONST_____:
				stack[sp++] = number(insn.op0);
				break;
			case POP___________:
				regs[insn.op0] = stack[--sp];
				break;
			case REMARK________:
				break;
			case RETURN________:
				current = current.previous;
				break;
			case RETURNVALUE___:
				Node returnValue = regs[insn.op0]; // Saves return value
				current = current.previous;
				current.frame.registers[instructions[current.ip - 1].op0] = returnValue;
				break;
			case SETCLOSURERES_:
				((Closure) regs[insn.op0]).result = regs[insn.op1];
				break;
			case TOP___________:
				regs[insn.op0] = stack[sp + insn.op1];
				break;
			default:
				exec.current = current;
				exec.sp = sp;
				execute(exec, insn);
				current = exec.current;
				sp = exec.sp;
			}
		}
	}

	protected class Exec {
		protected Activation current;
		protected Object stack[];
		protected int sp;
	}

	protected void execute(Exec exec, Instruction insn) {
		throw new RuntimeException("Unknown instruction " + insn);
	}

	protected static Int number(int n) {
		return Int.create(n);
	}

	protected static Atom atom(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	protected static int i(Object node) {
		return ((Int) node).getNumber();
	}

}
