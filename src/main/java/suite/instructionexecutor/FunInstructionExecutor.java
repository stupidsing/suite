package suite.instructionexecutor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.instructionexecutor.InstructionUtil.Activation;
import suite.instructionexecutor.InstructionUtil.Closure;
import suite.instructionexecutor.InstructionUtil.Frame;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.invocable.Invocable;
import suite.lp.invocable.InvocableBridge;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;
import suite.node.util.Comparer;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class FunInstructionExecutor extends InstructionExecutor {

	private Fun<Node, Node> unwrapper = new Fun<Node, Node>() {
		public Node apply(Node node) {
			return unwrap0(node);
		}
	};

	private InvocableBridge invocableBridge = new InvocableBridge() {
		public Fun<Node, Node> getUnwrapper() {
			return unwrapper;
		}

		public Node wrapInvocable(Invocable invocable, Node node) {
			return wrapInvocable0(invocable, node);
		}
	};

	private Comparer comparer = new FunComparer(unwrapper);

	private int invokeJavaEntryPoint;

	public FunInstructionExecutor(Node node) {
		super(node);
	}

	public void executeToWriter(Writer writer) throws IOException {
		ExpandUtil.expandToWriter(unwrapper, execute(), writer);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		Object ds[] = exec.stack;
		int dsp = exec.sp;

		Node n0, n1, result;
		Data<?> data;

		switch (insn.insn) {
		case COMPARE_______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = Int.create(comparer.compare(n0, n1));
			break;
		case CONSLIST______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = Tree.create(TermOp.OR____, n0, n1);
			break;
		case CONSPAIR______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = Tree.create(TermOp.TUPLE_, n0, n1);
			break;
		case HEAD__________:
			result = Tree.decompose((Node) ds[--dsp]).getLeft();
			break;
		case INVOKEJAVACLS_:
			Atom atom = (Atom) ds[--dsp];
			String clazzName = atom.getName().split("!")[1];
			result = InstructionUtil.execInvokeJavaClass(clazzName);
			break;
		case INVOKEJAVAOBJ0:
		case INVOKEJAVAOBJ1:
		case INVOKEJAVAOBJ2:
		case INVOKEJAVAOBJ3:
			data = (Data<?>) ds[--dsp];
			List<Node> ps = new ArrayList<>(3);
			for (int i = 0; i < Util.charAt(insn.insn.name, -1) - '0'; i++)
				ps.add((Node) ds[--dsp]);
			Invocable invocable = Data.get(data);
			result = invocable.invoke(invocableBridge, ps);
			break;
		case ISCONS________:
			result = atom(Tree.decompose((Node) ds[--dsp]) != null);
			break;
		case TAIL__________:
			result = Tree.decompose((Node) ds[--dsp]).getRight();
			break;
		default:
			throw new RuntimeException("Unknown instruction " + insn);
		}

		exec.sp = dsp;
		regs[insn.op0] = result;
	}

	@Override
	protected void postprocessInstructions(List<Instruction> list) {
		list.add(new Instruction(Insn.ENTER_________, 3, 0, 0));

		invokeJavaEntryPoint = list.size();
		list.add(new Instruction(Insn.PUSH__________, 0, 0, 0));
		list.add(new Instruction(Insn.PUSH__________, 1, 0, 0));
		list.add(new Instruction(Insn.INVOKEJAVAOBJ1, 2, 2, 0));
		list.add(new Instruction(Insn.RETURNVALUE___, 2, 0, 0));
		list.add(new Instruction(Insn.LEAVE_________, 0, 0, 0));

		super.postprocessInstructions(list);
	}

	@Override
	protected Comparer comparer() {
		return comparer;
	}

	private Node unwrap0(Node node) {
		node = node.finalNode();
		if (node instanceof Closure)
			node = evaluateClosure((Closure) node);
		return node;
	}

	private Node wrapInvocable0(Invocable invocable, Node node) {
		Frame frame = new Frame(null, 3);
		frame.registers[0] = node;
		frame.registers[1] = new Data<>(invocable);
		return new Closure(frame, FunInstructionExecutor.this.invokeJavaEntryPoint);
	}

	public Fun<Node, Node> getUnwrapper() {
		return unwrapper;
	}

	@Override
	public void close() {
	}

}
