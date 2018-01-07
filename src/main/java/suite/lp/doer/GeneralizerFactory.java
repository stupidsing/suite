package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface GeneralizerFactory {

	public Env env();

	public Generalize_ generalizer(Node node);

	public interface Generalize_ {
		public Node apply(Env env);
	}

}
