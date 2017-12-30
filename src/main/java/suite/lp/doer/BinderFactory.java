package suite.lp.doer;

import suite.lp.Trail;
import suite.lp.sewing.Env;
import suite.node.Node;

public interface BinderFactory extends ClonerFactory {

	public interface BindEnv {
		public Env getEnv();

		public Trail getTrail();
	}

	public interface BindPredicate {
		public boolean test(BindEnv be, Node node);
	}

	public BindPredicate compileBind(Node node);

}
