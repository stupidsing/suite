package suite.instructionexecutor;

import primal.fp.Funs.Iterate;
import primal.primitive.adt.Chars;
import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.InstructionUtil.FunComparer;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.*;
import suite.node.util.Comparer;
import suite.util.To;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import static primal.statics.Fail.fail;

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
		Data<?> data;

		Node result = switch (insn.insn) {
		case CALLINTRINSIC_ -> {
			data = (Data<?>) ds[--dsp];
			var ps = new ArrayList<Node>(3);
			for (var i = 1; i < insn.op1; i++)
				ps.add((Node) ds[--dsp]);
			yield Data.<Intrinsic> get(data).invoke(intrinsicCallback, ps);
		}
		case COMPARE_______ -> {
			var n0 = (Node) ds[--dsp];
			var n1 = (Node) ds[--dsp];
			yield Int.of(comparer.compare(n0, n1));
		}
		case CONSLIST______ -> {
			var n0 = (Node) ds[--dsp];
			var n1 = (Node) ds[--dsp];
			yield Tree.ofOr(n0, n1);
		}
		case CONSPAIR______ -> {
			var n0 = (Node) ds[--dsp];
			var n1 = (Node) ds[--dsp];
			yield Tree.ofAnd(n0, n1);
		}
		case DATACHARS_____ -> new Data<>(To.chars(Str.str(regs[insn.op1])));
		case GETINTRINSIC__ -> {
			var atom = (Atom) ds[--dsp];
			var intrinsicName = atom.name.split("!")[1];
			yield new Data<>(Intrinsics.intrinsics.get(intrinsicName));
		}
		case HEAD__________ -> Tree.decompose((Node) ds[--dsp]).getLeft();
		case ISCONS________ -> atom(Tree.decompose((Node) ds[--dsp]) != null);
		case TAIL__________ -> Tree.decompose((Node) ds[--dsp]).getRight();
		default -> fail("unknown instruction " + insn);
		};

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
