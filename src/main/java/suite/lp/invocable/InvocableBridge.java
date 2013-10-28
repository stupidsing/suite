package suite.lp.invocable;

import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface InvocableBridge {

	public Fun<Node, Node> getUnwrapper();

	public Node wrapInvocableNode(Invocable invocable, Node node);

}
