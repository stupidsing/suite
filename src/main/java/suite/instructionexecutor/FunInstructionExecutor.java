package suite.instructionexecutor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.InstructionUtil.Activation;
import suite.instructionexecutor.InstructionUtil.Frame;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.primitive.Chars;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class FunInstructionExecutor extends InstructionExecutor {

	private IntrinsicCallback intrinsicCallback;
	private Comparer comparer;

	public FunInstructionExecutor(Node node) {
		super(node);
	}

	public void executeToCharsWriter(Writer writer) throws IOException {
		ThunkUtil.yawnSink(intrinsicCallback::yawn, execute(), n -> {
			Data.<Chars> get((Data<?>) n).write(writer);
			writer.flush();
		});
	}

	public void executeToWriter(Writer writer) throws IOException {
		ThunkUtil.yawnWriter(intrinsicCallback::yawn, execute(), writer);
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
			result = Data.<Intrinsic> get(data).invoke(intrinsicCallback, ps);
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
			result = new Data<>(To.chars(((Str) regs[insn.op1]).value));
			break;
		case GETINTRINSIC__:
			Atom atom = (Atom) ds[--dsp];
			String intrinsicName = atom.name.split("!")[1];
			result = new Data<>(Intrinsics.intrinsics.get(intrinsicName));
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
	protected Comparer comparer() {
		return comparer;
	}

	public Fun<Node, Node> getYawnFun() {
		return intrinsicCallback::yawn;
	}

	@Override
	public void close() {
	}

	protected void setIntrinsicCallback(IntrinsicCallback intrinsicCallback) {
		this.intrinsicCallback = intrinsicCallback;
		comparer = new FunComparer(intrinsicCallback::yawn);
	}

}
