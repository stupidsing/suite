package suite.instructionexecutor;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.InstructionUtil.Frame;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.instructionexecutor.InstructionUtil.Thunk;
import suite.node.Data;
import suite.node.Node;

public class LazyFunInstructionExecutor extends FunInstructionExecutor {

	private int invokeJavaEntryPoint;

	public LazyFunInstructionExecutor(Node node) {
		super(node);

		setIntrinsicCallback(new IntrinsicCallback() {
			public Node enclose(Intrinsic intrinsic, Node node) {
				var frame = new Frame(null, new Node[] { node, new Data<>(intrinsic), null, });
				return new Thunk(frame, invokeJavaEntryPoint);
			}

			public Node yawn(Node node) {
				return node instanceof Thunk thunk ? yawnThunk(thunk) : node;
			}
		});
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

}
