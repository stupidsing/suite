package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;

public interface ClonerFactory {

	public Env env();

	public Clone_ cloner(Node node);

	public interface Clone_ {
		public Node apply(Env env);
	}

}
