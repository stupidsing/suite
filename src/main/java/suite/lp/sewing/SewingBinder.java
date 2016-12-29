package suite.lp.sewing;

import suite.lp.Trail;
import suite.node.Node;

public interface SewingBinder extends SewingCloner {

	public interface BindPredicate {
		public boolean test(BindEnv be, Node node);
	}

	public interface BindEnv {
		public Env getEnv();

		public Trail getTrail();
	}

	public BindPredicate compileBind(Node node);

}
