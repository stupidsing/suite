package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface EvaluatorFactory {

	public Evaluate_ evaluator(Node node);

	public interface Evaluate_ {
		public int evaluate(Env env);
	}

}
