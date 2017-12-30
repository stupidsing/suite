package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface EvaluatorFactory {

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public Evaluate compile(Node node);

}
