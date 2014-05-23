package suite.lp.intrinsic;

import suite.node.Node;

public interface IntrinsicBridge {

	public Node unwrap(Node node);

	public Node wrap(Intrinsic intrinsic, Node node);

}
