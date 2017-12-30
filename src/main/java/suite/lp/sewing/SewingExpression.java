package suite.lp.sewing;

import suite.node.Node;

public interface SewingExpression {

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public Evaluate compile(Node node);

}
