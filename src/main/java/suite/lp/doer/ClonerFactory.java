package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface ClonerFactory {

	public interface Clone_ {
		public Node apply(Env env);
	}

	public Env env();

	public Clone_ compile(Node node);

}
