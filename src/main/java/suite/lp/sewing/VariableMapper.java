package suite.lp.sewing;

import suite.node.Node;
import suite.node.Reference;

public interface VariableMapper {

	public static class Env {
		public final Reference refs[];

		public Env(Reference refs[]) {
			this.refs = refs;
		}

		public Node get(int index) {
			return getReference(index).finalNode();
		}

		public Reference getReference(int index) {
			return refs[index];
		}
	}

	public Env env();

}
