package suite.instructionexecutor.fun;

import suite.instructionexecutor.ExpandUtil;
import suite.node.Int;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class InvocableFun {

	public interface InvocableJavaFun {
		public Node invoke(Fun<Node, Node> unwrapper, Node input);
	}

	public static class StringLength implements InvocableJavaFun {
		public Node invoke(Fun<Node, Node> unwrapper, Node input) {
			return Int.create(ExpandUtil.expandString(input, unwrapper).length());
		}
	}

}
