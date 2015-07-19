package suite.lp.sewing.impl;

import suite.lp.kb.Prototype;
import suite.lp.sewing.QueryRewriter;
import suite.node.Node;

public class QueryNoRewriterImpl implements QueryRewriter {

	@Override
	public Node rewrite(Prototype prototype, Node node) {
		return node;
	}

	@Override
	public Prototype getPrototype(Prototype prototype0, Node node, int n) {
		return Prototype.of(node, n);
	}

}
