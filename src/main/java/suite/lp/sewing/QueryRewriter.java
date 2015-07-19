package suite.lp.sewing;

import suite.lp.kb.Prototype;
import suite.node.Node;

public interface QueryRewriter {

	public Node rewrite(Node node);

	public Prototype getPrototype(Prototype prototype0, Node node, int n);

}
