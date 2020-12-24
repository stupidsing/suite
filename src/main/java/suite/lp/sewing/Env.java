package suite.lp.sewing;

import primal.Verbs.New;
import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Reference;

public class Env {

	public final Reference[] refs;

	public static Env empty(int n) {
		return new Env(New.array(n, Reference.class, i -> new Reference()));
	}

	public Env(Reference[] refs) {
		this.refs = refs;
	}

	public Env clone() {
		var cloner = new Cloner();
		return new Env(New.array(refs.length, Reference.class, i -> Reference.of(cloner.clone(refs[i]))));
	}

	public Node get(int index) {
		return refs[index].finalNode();
	}

}
