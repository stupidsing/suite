package suite.lp.invocable;

import java.util.List;

import suite.node.Node;

public interface Invocable {

	public Node invoke(InvocableBridge bridge, List<Node> inputs);

}
