package suite.lp.doer;

import primal.Verbs.Equals;
import primal.Verbs.Union;
import suite.lp.Trail;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;

public class Binder {

	public static boolean bind(Node n0, Node n1) {
		var trail = new Trail();
		var b = bind(n0, n1, trail);
		if (!b)
			trail.unwindAll();
		return b;
	}

	public static boolean bind(Node n0, Node n1, Trail trail) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();

		if (n0 == n1)
			return true;

		if (n0 instanceof Reference ref) {
			trail.addBind(ref, n1);
			return true;
		} else if (n1 instanceof Reference ref) {
			trail.addBind(ref, n0);
			return true;
		}

		if (n0 instanceof Dict d0 && n1 instanceof Dict d1) {
			bind(d0.reference, d1.reference, trail);

			var map0 = d0.getMap();
			var map1 = d1.getMap();
			var b = true;

			for (var key : Union.of(map0.keySet(), map1.keySet())) {
				var v0 = map0.computeIfAbsent(key, k -> new Reference());
				var v1 = map1.computeIfAbsent(key, k -> new Reference());
				b &= bind(v0, v1, trail);
			}

			return b;
		} else if (n0 instanceof Int i0 && n1 instanceof Int i1)
			return i0.number == i1.number;
		else if (n0 instanceof Str s0 && n1 instanceof Str s1)
			return Equals.ab(s0.value, s1.value);
		else if (n0 instanceof Tree t0 && n1 instanceof Tree t1) {
			return t0.getOperator() == t1.getOperator() //
					&& bind(t0.getLeft(), t1.getLeft(), trail) //
					&& bind(t0.getRight(), t1.getRight(), trail);
		} else if (n0 instanceof Tuple t0 && n1 instanceof Tuple t1) {
			var nodes0 = t0.nodes;
			var nodes1 = t1.nodes;
			var b = nodes0.length == nodes1.length;
			if (b)
				for (var i = 0; i < nodes0.length; i++)
					b &= bind(nodes0[i], nodes1[i], trail);
			return b;
		} else
			return false;
	}

}
