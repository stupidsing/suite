package suite.funp.p0;

import primal.persistent.PerMap;
import suite.funp.Funp_;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.Prototype;
import suite.node.Node;
import suite.node.Tree;

public class P01Expand {

	private PerMap<Prototype, Node[]> macros;

	public P01Expand(PerMap<Prototype, Node[]> macros) {
		this.macros = macros;
	}

	public Node e(Node node) {
		Node[] m;

		if ((m = Funp_.pattern("expand €0 := €1 ~ €2").match(node)) != null) {
			var head = m[0];
			return new P01Expand(macros.put(Prototype.of(head), new Node[] { head, m[1], })).e(m[2]);
		} else if ((m = macros.getOrNull(Prototype.of(node))) != null) {
			var g = new Generalizer("%");
			var t0 = g.generalize(m[0]);
			var t1 = g.generalize(m[1]);

			if (Binder.bind(node, t0))
				return e(t1);
		}

		var tree = Tree.decompose(node);

		return tree != null ? Tree.of(tree.getOperator(), e(tree.getLeft()), e(tree.getRight())) : node;
	}

}
