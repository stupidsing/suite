package suite.instructionexecutor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOr;
import suite.node.util.Comparer;
import suite.primitive.Chars;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;
import suite.util.To;

public class FunInstructionExecutor extends InstructionExecutor {

	private IntrinsicCallback intrinsicCallback;
	private Comparer comparer;

	public FunInstructionExecutor(Node node) {
		super(node);
	}

	public void executeToWriter(Writer writer) throws IOException {
		ThunkUtil.yawnSink(intrinsicCallback::yawn, execute(), n -> {
			Data.<Chars> get((Data<?>) n).write(writer::write);
			writer.flush();
		});
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		var current = exec.current;
		var frame = current.frame;
		var regs = frame != null ? frame.registers : null;

		var ds = exec.stack;
		var dsp = exec.sp;

		Node n0, n1, result;
		Data<?> data;

		switch (insn.insn) {
		case CALLINTRINSIC_:
			data = (Data<?>) ds[--dsp];
			var ps = new ArrayList<Node>(3);
			for (var i = 1; i < insn.op1; i++)
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
			result = TreeOr.of(n0, n1);
			break;
		case CONSPAIR______:
			n0 = (Node) ds[--dsp];
			n1 = (Node) ds[--dsp];
			result = TreeAnd.of(n0, n1);
			break;
		case DATACHARS_____:
			result = new Data<>(To.chars(Str.str(regs[insn.op1])));
			break;
		case GETINTRINSIC__:
			var atom = (Atom) ds[--dsp];
			var intrinsicName = atom.name.split("!")[1];
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
			result = Fail.t("unknown instruction " + insn);
		}

		exec.sp = dsp;
		regs[insn.op0] = result;
	}

	@Override
	protected Comparer comparer() {
		return comparer;
	}

	public Iterate<Node> getYawnFun() {
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
