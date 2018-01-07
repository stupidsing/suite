package suite.lp.doer;

import suite.lp.Trail;
import suite.lp.sewing.Env;
import suite.node.Node;

public interface BinderFactory extends ClonerFactory {

	public Bind_ binder(Node node);

	public interface Bind_ {
		public boolean test(BindEnv be, Node node);
	}

	public class BindEnv {
		public Env env;
		public final Trail trail;

		public BindEnv(Env env) {
			this.env = env;
			trail = new Trail();
		}
	}

}
