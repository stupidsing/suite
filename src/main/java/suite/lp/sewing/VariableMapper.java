package suite.lp.sewing;

import suite.node.Reference;

public interface VariableMapper {

	public static class Env {
		public final Reference refs[];

		public Env(Reference refs[]) {
			this.refs = refs;
		}

		public Reference get(int index) {
			return refs[index];
		}
	}

	public Env env();

}
