package suite.lp.intrinsic;

import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface IntrinsicBridge {

	public Fun<Node, Node> getUnwrapper();

	public Node wrapIntrinsic(Intrinsic intrinsic, Node node);

}
