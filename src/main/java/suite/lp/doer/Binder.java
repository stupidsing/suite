package suite.lp.doer;

import primal.Verbs.Equals;
import primal.Verbs.Union;
import suite.lp.Trail;
import suite.node.*;

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

		var clazz0 = n0.getClass();
		var clazz1 = n1.getClass();

		if (clazz0 == Reference.class) {
			trail.addBind((Reference) n0, n1);
			return true;
		} else if (clazz1 == Reference.class) {
			trail.addBind((Reference) n1, n0);
			return true;
		}

		if (clazz0 == Dict.class && clazz1 == Dict.class) {
			var dict0 = (Dict) n0;
			var dict1 = (Dict) n1;
			bind(dict0.reference, dict1.reference, trail);

			var map0 = dict0.getMap();
			var map1 = dict1.getMap();
			var b = true;

			for (var key : Union.of(map0.keySet(), map1.keySet())) {
				var v0 = map0.computeIfAbsent(key, k -> new Reference());
				var v1 = map1.computeIfAbsent(key, k -> new Reference());
				b &= bind(v0, v1, trail);
			}

			return b;
		} else if (clazz0 == Int.class && clazz1 == Int.class)
			return Int.num(n0) == Int.num(n1);
		else if (clazz0 == Str.class && clazz1 == Str.class)
			return Equals.ab(Str.str(n0), Str.str(n1));
		else if (Tree.class.isAssignableFrom(clazz0) && Tree.class.isAssignableFrom(clazz1)) {
			var t0 = (Tree) n0;
			var t1 = (Tree) n1;
			return t0.getOperator() == t1.getOperator()
					&& bind(t0.getLeft(), t1.getLeft(), trail)
					&& bind(t0.getRight(), t1.getRight(), trail);
		} else if (clazz0 == Tuple.class && clazz1 == Tuple.class) {
			var nodes0 = Tuple.t(n0);
			var nodes1 = Tuple.t(n1);
			var b = nodes0.length == nodes1.length;
			if (b)
				for (var i = 0; i < nodes0.length; i++)
					b &= bind(nodes0[i], nodes1[i], trail);
			return b;
		} else
			return false;
	}

}
