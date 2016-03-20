package suite.instructionexecutor;

import suite.fp.intrinsic.Intrinsics;
import suite.node.Node;

public class EagerFunInstructionExecutor extends FunInstructionExecutor {

	public EagerFunInstructionExecutor(Node node) {
		super(node);
		setIntrinsicCallback(Intrinsics.eagerIntrinsicCallback);
	}

}
