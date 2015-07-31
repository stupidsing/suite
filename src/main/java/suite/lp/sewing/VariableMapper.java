package suite.lp.sewing;

import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Reference;

public interface VariableMapper {

	public static class Env {
		public final Reference refs[];

		public Env(Reference refs[]) {
			this.refs = refs;
		}

		public Env clone() {
			Reference refs1[] = new Reference[refs.length];
			Env env1 = new Env(refs1);
			Cloner cloner = new Cloner();

			for (int i = 0; i < refs.length; i++)
				refs1[i] = Reference.of(cloner.clone(refs[i]));

			return env1;
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
