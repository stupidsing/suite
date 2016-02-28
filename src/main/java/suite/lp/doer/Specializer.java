package suite.lp.doer;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewriter;

public class Specializer {

	public Node specialize(Node node) {
		if (node instanceof Reference) {
			Reference ref = (Reference) node;
			node = Atom.of(ProverConstant.variablePrefix + ref.getId());
		} else
			node = Rewriter.map(node, this::specialize);
		return node;
	}

}
