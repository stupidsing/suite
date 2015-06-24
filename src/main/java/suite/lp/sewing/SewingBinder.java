package suite.lp.sewing;

import java.util.function.BiPredicate;

import suite.lp.Journal;
import suite.node.Node;

public interface SewingBinder extends SewingCloner {

	public static class BindEnv {
		public final Journal journal;
		public final Env env;

		public BindEnv(Journal journal, Env env) {
			this.journal = journal;
			this.env = env;
		}
	}

	public BiPredicate<BindEnv, Node> compileBind(Node node);

}
