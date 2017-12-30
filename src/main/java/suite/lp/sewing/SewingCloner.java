package suite.lp.sewing;

import suite.node.Node;

public interface SewingCloner {

	public interface Clone_ {
		public Node apply(Env env);
	}

	public Env env();

	public Clone_ compile(Node node);

}
