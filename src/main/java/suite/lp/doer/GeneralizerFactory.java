package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface GeneralizerFactory {

	public interface Generalize_ {
		public Node apply(Env env);
	}

	public Env env();

	public Generalize_ compile(Node node);

}
