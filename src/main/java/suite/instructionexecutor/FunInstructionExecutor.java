package suite.instructionexecutor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.InstructionUtil.Activation;
import suite.instructionexecutor.InstructionUtil.Frame;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.instructionexecutor.InstructionUtil.Thunk;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class FunInstructionExecutor extends InstructionExecutor {

	private IntrinsicCallback intrinsicCallback;
	private Comparer comparer;

	private int invokeJavaEntryPoint;

	public FunInstructionExecutor(Node node, boolean isLazy) {
		super(node);

		if (isLazy)
			intrinsicCallback = new IntrinsicCallback() {
				public Node yawn(Node node) {
					node = node.finalNode();
					return node instanceof Thunk ? evaluateThunk((Thunk) node) : node;
				}

				public Node enclose(Intrinsic intrinsic, Node node) {
					Frame frame = new Frame(null, new Node[] { node, new Data<>(intrinsic), null });
					return new Thunk(frame, invokeJavaEntryPoint);
				}
			};
		else
			intrinsicCallback = new IntrinsicCallback() {
				public Node yawn(Node node) {
					return node;
				}

				public Node enclose(Intrinsic intrinsic, Node node) {
					return intrinsic.invoke(this, Arrays.asList(node));
				}
			};

		comparer = new FunComparer(intrinsicCallback::yawn);
	}

	public void executeToWriter(Writer writer) throws IOException {
		ThunkUtil.evaluateToWriter(intrinsicCallback::yawn, execute(), writer);
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
			result = intrinsic.invoke(intrinsicCallback, ps);
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
		case DATASTRING____:
			result = new Data<>(To.chars(((Str) regs[insn.op1]).getValue()));
			break;
		case GETINTRINSIC__:
			Atom atom = (Atom) ds[--dsp];
			String intrinsicName = atom.getName().split("!")[1];
			result = InstructionUtil.execGetIntrinsic(intrinsicName);
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
		return intrinsicCallback::yawn;
	}

	@Override
	public void close() {
	}

}
