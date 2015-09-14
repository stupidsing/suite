package suite.lp.sewing;

import java.util.function.BiPredicate;

import suite.lp.Trail;
import suite.node.Node;

public interface SewingBinder extends SewingCloner {

	public interface BindEnv {
		public Env getEnv();

		public Trail getTrail();
	}

	public BiPredicate<BindEnv, Node> compileBind(Node node);

}
