package suite.instructionexecutor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.instructionexecutor.InstructionUtil.Activation;
import suite.instructionexecutor.InstructionUtil.Closure;
import suite.instructionexecutor.InstructionUtil.Frame;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.intrinsic.Intrinsic;
import suite.lp.intrinsic.IntrinsicBridge;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.util.FunUtil.Fun;

public class FunInstructionExecutor extends InstructionExecutor {

	private IntrinsicBridge intrinsicBridge;
	private Comparer comparer;

	private int invokeJavaEntryPoint;

	public FunInstructionExecutor(Node node, boolean isLazy) {
		super(node);

		if (isLazy)
			intrinsicBridge = new IntrinsicBridge() {
				public Node unwrap(Node node) {
					node = node.finalNode();
					return node instanceof Closure ? evaluateClosure((Closure) node) : node;
				}

				public Node wrap(Intrinsic intrinsic, Node node) {
					Frame frame = new Frame(null, new Node[] { node, new Data<>(intrinsic), null });
					return new Closure(frame, invokeJavaEntryPoint);
				}
			};
		else
			intrinsicBridge = new IntrinsicBridge() {
				public Node unwrap(Node node) {
					return node;
				}

				public Node wrap(Intrinsic intrinsic, Node node) {
					return intrinsic.invoke(this, Arrays.asList(node));
				}
			};

		comparer = new FunComparer(intrinsicBridge::unwrap);
	}

	public void executeToWriter(Writer writer) throws IOException {
		ExpandUtil.expandToWriter(intrinsicBridge::unwrap, execute(), writer);
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
		case CALLINTRINSIC_:
			data = (Data<?>) ds[--dsp];
			List<Node> ps = new ArrayList<>(3);
			for (int i = 1; i < insn.op1; i++)
				ps.add((Node) ds[--dsp]);
			Intrinsic intrinsic = Data.get(data);
			result = intrinsic.invoke(intrinsicBridge, ps);
			break;
		case COMPARE_______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = Int.of(comparer.compare(n0, n1));
			break;
		case CONSLIST______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = Tree.of(TermOp.OR____, n0, n1);
			break;
		case CONSPAIR______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = Tree.of(TermOp.AND___, n0, n1);
			break;
		case GETINTRINSIC__:
			Atom atom = (Atom) ds[--dsp];
			String clazzName = atom.getName().split("!")[1];
			result = InstructionUtil.execInvokeJavaClass(clazzName);
			break;
		case HEAD__________:
			result = Tree.decompose((Node) ds[--dsp]).getLeft();
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
		invokeJavaEntryPoint = list.size();

		list.add(new Instruction(Insn.FRAMEBEGIN____, 3, 0, 0));
		list.add(new Instruction(Insn.PUSH__________, 0, 0, 0));
		list.add(new Instruction(Insn.PUSH__________, 1, 0, 0));
		list.add(new Instruction(Insn.CALLINTRINSIC_, 2, 2, 0));
		list.add(new Instruction(Insn.SETRESULT_____, 2, 0, 0));
		list.add(new Instruction(Insn.RETURN________, 0, 0, 0));
		list.add(new Instruction(Insn.FRAMEEND______, 0, 0, 0));

		super.postprocessInstructions(list);
	}

	@Override
	protected Comparer comparer() {
		return comparer;
	}

	public Fun<Node, Node> getUnwrapper() {
		return intrinsicBridge::unwrap;
	}

	@Override
	public void close() {
	}

}
