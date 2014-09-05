package suite.lp.intrinsic;

import suite.lp.intrinsic.Intrinsics.Intrinsic;
import suite.lp.intrinsic.Intrinsics.IntrinsicBridge;
import suite.node.Node;
import suite.node.Tree;

public class SeqIntrinsics {

	public Intrinsic deepSeq = (bridge, inputs) -> deepSeq(bridge, inputs.get(0));

	public Intrinsic seq = (bridge, inputs) -> {
		bridge.unwrap(inputs.get(0));
		return inputs.get(1);
	};

	private Node deepSeq(IntrinsicBridge bridge, Node node) {
		Node node1 = bridge.unwrap(node);
		Tree tree;
		if ((tree = Tree.decompose(node1)) != null)
			node1 = Tree.of(tree.getOperator(), deepSeq(bridge, tree.getLeft()), deepSeq(bridge, tree.getRight()));
		return bridge.wrap(Intrinsics.id_, node1);
	}

}
