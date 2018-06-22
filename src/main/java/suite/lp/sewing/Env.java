package suite.lp.sewing;

import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Reference;
import suite.util.To;

public class Env {

	public final Reference[] refs;

	public static Env empty(int n) {
		return new Env(To.array(n, Reference.class, i -> new Reference()));
	}

	public Env(Reference[] refs) {
		this.refs = refs;
	}

	public Env clone() {
		var cloner = new Cloner();
		return new Env(To.array(refs.length, Reference.class, i -> Reference.of(cloner.clone(refs[i]))));
	}

	public Node get(int index) {
		return refs[index].finalNode();
	}

}
