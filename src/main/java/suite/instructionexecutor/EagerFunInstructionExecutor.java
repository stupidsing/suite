package suite.instructionexecutor;

import java.util.Arrays;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.node.Node;

public class EagerFunInstructionExecutor extends FunInstructionExecutor {

	public EagerFunInstructionExecutor(Node node) {
		super(node);

		setIntrinsicCallback(new IntrinsicCallback() {
			public Node enclose(Intrinsic intrinsic, Node node) {
				return intrinsic.invoke(this, Arrays.asList(node));
			}

			public Node yawn(Node node) {
				return node;
			}
		});
	}

}
