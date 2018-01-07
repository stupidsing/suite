package suite.lp.sewing;

import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Reference;

public class Env {

	public final Reference[] refs;

	public static Env empty(int n) {
		Reference[] refs = new Reference[n];
		for (int i = 0; i < n; i++)
			refs[i] = new Reference();
		return new Env(refs);
	}

	public Env(Reference[] refs) {
		this.refs = refs;
	}

	public Env clone() {
		Reference[] refs1 = new Reference[refs.length];
		Cloner cloner = new Cloner();
		for (int i = 0; i < refs.length; i++)
			refs1[i] = Reference.of(cloner.clone(refs[i]));
		return new Env(refs1);
	}

	public Node get(int index) {
		return refs[index].finalNode();
	}

}
