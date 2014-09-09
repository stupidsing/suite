package suite.fp.intrinsic;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.node.Node;
import suite.node.Tree;

public class SeqIntrinsics {

	public Intrinsic deepSeq = (callback, inputs) -> deepSeq(callback, inputs.get(0));

	public Intrinsic seq = (callback, inputs) -> {
		callback.unwrap(inputs.get(0));
		return inputs.get(1);
	};

	private Node deepSeq(IntrinsicCallback callback, Node node) {
		Node node1 = callback.unwrap(node);
		Tree tree;
		if ((tree = Tree.decompose(node1)) != null)
			node1 = Tree.of(tree.getOperator(), deepSeq(callback, tree.getLeft()), deepSeq(callback, tree.getRight()));
		return callback.wrap(Intrinsics.id_, node1);
	}

}
