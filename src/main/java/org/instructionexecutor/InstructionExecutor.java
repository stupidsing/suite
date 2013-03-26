package org.instructionexecutor;

import java.util.ArrayList;
import java.util.List;

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

	private Instruction instructions[];
	private int unwrapEntryPoint;

	protected BiMap<Integer, Node> constantPool = HashBiMap.create();
	private static final int stackSize = 4096;

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

		Closure current = new Closure(f0, unwrapEntryPoint);

		Closure callStack[] = new Closure[stackSize];
		Node dataStack[] = new Node[stackSize];
		int i, csp = 0, dsp = 0;

		Comparer comparer = new Comparer();

		for (;;) {
			Frame frame = current.frame;
			Node regs[] = frame != null ? frame.registers : null;
			int ip = current.ip++;
			Instruction insn = instructions[ip];

			// org.util.LogUtil.info("TRACE", ip + "> " + insn);

			switch (insn.insn) {
			case ASSIGNCLOSURE_:
				regs[insn.op1] = new Closure(frame, insn.op2);
				break;
			case ASSIGNFRAMEREG:
				i = insn.op2;
				while (i++ < 0)
					frame = frame.previous;
				regs[insn.op1] = frame.registers[insn.op3];
				break;
			case ASSIGNCONST___:
				regs[insn.op1] = constantPool.get(insn.op2);
				break;
			case ASSIGNINT_____:
				regs[insn.op1] = i(insn.op2);
				break;
			case CALL__________:
				callStack[csp++] = current;
				current = new Closure(frame, g(regs[insn.op1]));
				break;
			case CALLCONST_____:
				callStack[csp++] = current;
				current = new Closure(frame, insn.op1);
				break;
			case CALLCLOSURE___:
				Closure closure = (Closure) regs[insn.op2];
				if (closure.result == null) {
					callStack[csp++] = current;
					current = closure.clone();
				} else
					regs[insn.op1] = closure.result;
				break;
			case ENTER_________:
				current.frame = new Frame(frame, insn.op1);
				break;
			case EVALADD_______:
				regs[insn.op1] = i(g(regs[insn.op2]) + g(regs[insn.op3]));
				break;
			case EVALDIV_______:
				regs[insn.op1] = i(g(regs[insn.op2]) / g(regs[insn.op3]));
				break;
			case EVALEQ________:
				i = comparer.compare(regs[insn.op2], regs[insn.op3]);
				regs[insn.op1] = a(i == 0);
				break;
			case EVALGE________:
				i = comparer.compare(regs[insn.op2], regs[insn.op3]);
				regs[insn.op1] = a(i >= 0);
				break;
			case EVALGT________:
				i = comparer.compare(regs[insn.op2], regs[insn.op3]);
				regs[insn.op1] = a(i > 0);
				break;
			case EVALLE________:
				i = comparer.compare(regs[insn.op2], regs[insn.op3]);
				regs[insn.op1] = a(i <= 0);
				break;
			case EVALLT________:
				i = comparer.compare(regs[insn.op2], regs[insn.op3]);
				regs[insn.op1] = a(i < 0);
				break;
			case EVALNE________:
				i = comparer.compare(regs[insn.op2], regs[insn.op3]);
				regs[insn.op1] = a(i != 0);
				break;
			case EVALMOD_______:
				regs[insn.op1] = i(g(regs[insn.op2]) % g(regs[insn.op3]));
				break;
			case EVALMUL_______:
				regs[insn.op1] = i(g(regs[insn.op2]) * g(regs[insn.op3]));
				break;
			case EVALSUB_______:
				regs[insn.op1] = i(g(regs[insn.op2]) - g(regs[insn.op3]));
				break;
			case EXIT__________:
				return (Node) regs[insn.op1];
			case FORMTREE0_____:
				Node left = (Node) regs[insn.op1];
				Node right = (Node) regs[insn.op2];
				insn = instructions[current.ip++];
				String op = ((Atom) constantPool.get(insn.op1)).getName();
				regs[insn.op2] = Tree.create(TermOp.find(op), left, right);
				break;
			case IFFALSE_______:
				if (regs[insn.op2] != Atom.TRUE)
					current.ip = insn.op1;
				break;
			case IFNOTEQUALS___:
				if (regs[insn.op2] != regs[insn.op3])
					current.ip = insn.op1;
				break;
			case JUMP__________:
				current.ip = insn.op1;
				break;
			case LABEL_________:
				break;
			case LOG___________:
				LogUtil.info("EXEC", constantPool.get(insn.op1).toString());
				break;
			case NEWNODE_______:
				regs[insn.op1] = new Reference();
				break;
			case PUSH__________:
				dataStack[dsp++] = regs[insn.op1];
				break;
			case PUSHCONST_____:
				dataStack[dsp++] = i(insn.op1);
				break;
			case POP___________:
				regs[insn.op1] = dataStack[--dsp];
				break;
			case REMARK________:
				break;
			case RETURN________:
				current = callStack[--csp];
				break;
			case RETURNVALUE___:
				Node returnValue = regs[insn.op1]; // Saves return value
				current = callStack[--csp];
				current.frame.registers[instructions[current.ip - 1].op1] = returnValue;
				break;
			case SETCLOSURERES_:
				((Closure) regs[insn.op1]).result = regs[insn.op2];
				break;
			case TOP___________:
				regs[insn.op1] = dataStack[dsp + insn.op2];
				break;
			default:
				int pair[] = execute( //
						current, insn, callStack, csp, dataStack, dsp);
				csp = pair[0];
				dsp = pair[1];
			}
		}
	}

	protected int[] execute(Closure current, Instruction insn,
			Closure callStack[], int csp, Object dataStack[], int dsp) {
		throw new RuntimeException("Unknown instruction " + insn);
	}

	protected static Int i(int n) {
		return Int.create(n);
	}

	protected static Atom a(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	protected static int g(Object node) {
		return ((Int) node).getNumber();
	}

}
