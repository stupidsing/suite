package suite.instructionexecutor;

import primal.adt.map.BiHashMap;
import primal.adt.map.BiMap;
import primal.os.Log_;
import suite.Suite;
import suite.instructionexecutor.InstructionUtil.*;
import suite.node.*;
import suite.node.io.TermOp;
import suite.node.util.Comparer;

import java.util.ArrayList;
import java.util.List;

import static primal.statics.Fail.fail;

public class InstructionExecutor implements AutoCloseable {

	private Instruction[] instructions;
	private int yawnEntryPoint;

	protected BiMap<Integer, Node> constantPool = new BiHashMap<>();

	private InstructionAnalyzer analyzer = new InstructionAnalyzer();

	public InstructionExecutor(Node node) {
		var list = new ArrayList<Instruction>();

		try (var extractor = new InstructionExtractor(constantPool)) {
			list.addAll(extractor.extractInstructions(node));
		}

		if (Suite.isInstructionDump)
			for (var i = 0; i < list.size(); i++)
				System.err.println(i + ": " + list.get(i));

		postprocessInstructions(list);
		analyzer.analyze(list);
		analyzer.transform(list);
		instructions = list.toArray(new Instruction[list.size()]);
	}

	public Node execute() {
		return yawnThunk(new Thunk(null, 0));
	}

	public Node yawnThunk(Thunk thunk) {
		if (thunk.result == null) {
			thunk.result = yawnThunk_(thunk);
			thunk.frame = null; // facilitates garbage collection
		}
		return thunk.result;
	}

	private Node yawnThunk_(Thunk thunk0) {
		var f0 = new Frame(null, 2);
		f0.registers[0] = thunk0;

		var current = new Activation(f0, yawnEntryPoint, null);

		var stack = new Node[Suite.stackSize];
		int ip = 0, sp = 0;
		Node returnValue = null;

		var exec = new Exec();
		exec.stack = stack;

		var comparer = comparer();
		Tree tree;

		while (true)
			try {
				var frame = current.frame;
				var regs = frame != null ? frame.registers : null;
				var insn = instructions[ip = current.ip++];
				Thunk thunk;
				TermOp op;
				int i;

				switch (insn.insn) {
				case ASSIGNCONST___ -> {
					regs[insn.op0] = constantPool.get(insn.op1);
				}
				case ASSIGNFRAMEREG -> {
					i = insn.op1;
					while (i++ < 0)
						frame = frame.previous;
					regs[insn.op0] = frame.registers[insn.op2];
				}
				case ASSIGNINT_____ -> {
					regs[insn.op0] = number(insn.op1);
				}
				case ASSIGNRESULT__ -> {
					regs[insn.op0] = returnValue;
				}
				case ASSIGNTHUNK___ -> {
					regs[insn.op0] = new Thunk(frame, insn.op1);
				}
				case ASSIGNTHUNKRES -> {
					regs[insn.op0] = returnValue;
					thunk = (Thunk) regs[insn.op1];
					thunk.frame = null; // facilitates garbage collection
					thunk.result = returnValue;
				}
				case CALL__________ -> {
					current = new Activation(frame, insn.op0, current);
				}
				case CALLTHUNK_____ -> {
					thunk = (Thunk) regs[insn.op0];
					if (thunk.result == null)
						current = new Activation(thunk, current);
					else
						returnValue = thunk.result;
				}
				case ENTER_________ -> {
					var af = analyzer.getFrame(ip);
					Frame parent = af.isRequireParent() ? frame : null;
					var frameBegin = instructions[af.getFrameBeginIp()];
					current.frame = new Frame(parent, frameBegin.op0);
				}
				case ERROR_________ -> {
					fail("error termination");
				}
				case EVALADD_______ -> {
					regs[insn.op0] = number(Int.num(regs[insn.op1]) + Int.num(regs[insn.op2]));
				}
				case EVALDIV_______ -> {
					regs[insn.op0] = number(Int.num(regs[insn.op1]) / Int.num(regs[insn.op2]));
				}
				case EVALEQ________ -> {
					i = comparer.compare(regs[insn.op1], regs[insn.op2]);
					regs[insn.op0] = atom(i == 0);
				}
				case EVALLE________ -> {
					i = comparer.compare(regs[insn.op1], regs[insn.op2]);
					regs[insn.op0] = atom(i <= 0);
				}
				case EVALLT________ -> {
					i = comparer.compare(regs[insn.op1], regs[insn.op2]);
					regs[insn.op0] = atom(i < 0);
				}
				case EVALNE________ -> {
					i = comparer.compare(regs[insn.op1], regs[insn.op2]);
					regs[insn.op0] = atom(i != 0);
				}
				case EVALMOD_______ -> {
					regs[insn.op0] = number(Int.num(regs[insn.op1]) % Int.num(regs[insn.op2]));
				}
				case EVALMUL_______ -> {
					regs[insn.op0] = number(Int.num(regs[insn.op1]) * Int.num(regs[insn.op2]));
				}
				case EVALSUB_______ -> {
					regs[insn.op0] = number(Int.num(regs[insn.op1]) - Int.num(regs[insn.op2]));
				}
				case EXIT__________ -> {
					return returnValue;
				}
				case FORMTREE0_____ -> {
					var left = regs[insn.op0];
					var right = regs[insn.op1];
					insn = instructions[current.ip++];
					op = TermOp.find(Atom.name(constantPool.get(insn.op0)));
					regs[insn.op1] = Tree.of(op, left, right);
				}
				case FRAMEBEGIN____, FRAMEEND______ -> {
				}
				case IFFALSE_______ -> {
					if (regs[insn.op0] != Atom.TRUE)
						current.ip = insn.op1;
				}
				case IFNOTCONS_____ -> {
					if ((tree = Tree.decompose(regs[insn.op0], TermOp.OR____)) != null) {
						stack[sp++] = tree.getLeft();
						stack[sp++] = tree.getRight();
					} else
						current.ip = insn.op1;
				}
				case IFNOTPAIR_____ -> {
					if ((tree = Tree.decompose(regs[insn.op0], TermOp.AND___)) != null) {
						stack[sp++] = tree.getLeft();
						stack[sp++] = tree.getRight();
					} else
						current.ip = insn.op1;
				}
				case IFNOTEQUALS___ -> {
					if (regs[insn.op1] != regs[insn.op2])
						current.ip = insn.op0;
				}
				case JUMP__________ -> {
					current.ip = insn.op0;
				}
				case JUMPCLOSURE___ -> {
					thunk = (Thunk) regs[insn.op0];
					current = current.previous;
					if (thunk.result == null)
						current = new Activation(thunk, current);
					else
						returnValue = thunk.result;
				}
				case LEAVE_________ -> {
					current.frame = current.frame.previous;
				}
				case LOGREG________ -> {
					Log_.info(regs[insn.op0].toString());
				}
				case NEWNODE_______ -> {
					regs[insn.op0] = new Reference();
				}
				case PUSH__________ -> {
					stack[sp++] = regs[insn.op0];
				}
				case POP___________ -> {
					regs[insn.op0] = stack[--sp];
				}
				case POPANY________ -> {
					--sp;
				}
				case REMARK________ -> {
				}
				case RETURN________ -> {
					current = current.previous;
				}
				case SETRESULT_____ -> {
					returnValue = regs[insn.op0];
				}
				case TOP___________ -> {
					regs[insn.op0] = stack[sp + insn.op1];
				}
				default -> {
					exec.current = current;
					exec.sp = sp;
					handle(exec, insn);
					current = exec.current;
					sp = exec.sp;
				}
				}
			} catch (Exception ex) {
				fail("at IP = " + ip, ex);
			}
	}

	protected class Exec {
		protected Activation current;
		protected Object[] stack;
		protected int sp;
	}

	protected void handle(Exec exec, Instruction insn) {
		fail("unknown instruction " + insn);
	}

	protected void postprocessInstructions(List<Instruction> list) {
		yawnEntryPoint = list.size();
		list.add(new Instruction(Insn.FRAMEBEGIN____, 2, 0, 0));
		list.add(new Instruction(Insn.CALLTHUNK_____, 0, 0, 0));
		list.add(new Instruction(Insn.ASSIGNRESULT__, 1, 0, 0));
		list.add(new Instruction(Insn.EXIT__________, 0, 0, 0));
		list.add(new Instruction(Insn.FRAMEEND______, 0, 0, 0));
	}

	protected Comparer comparer() {
		return Comparer.comparer;
	}

	protected static Int number(int n) {
		return Int.of(n);
	}

	protected static Atom atom(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	@Override
	public void close() {
	}

	protected Instruction[] getInstructions() {
		return instructions;
	}

}
