package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface EvaluatorFactory {

	public interface Evaluate_ {
		public int evaluate(Env env);
	}

	public Evaluate_ evaluator(Node node);

}
