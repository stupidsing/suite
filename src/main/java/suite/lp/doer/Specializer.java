package suite.lp.doer;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewrite_;

public class Specializer {

	public Node specialize(Node node) {
		if (node instanceof Reference ref)
			node = Atom.of(ref.name());
		else
			node = Rewrite_.map(node, this::specialize);
		return node;
	}

}
