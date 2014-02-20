package suite.lp.intrinsic;

import java.util.List;

import suite.node.Node;

public interface Intrinsic {

	public Node invoke(IntrinsicBridge bridge, List<Node> inputs);

}
