package suite.node.util;

import static java.lang.Math.min;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import primal.Verbs.Compare;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOp;
import suite.node.tree.TreeOr;
import suite.node.tree.TreeTuple;
import suite.streamlet.Read;

public class Comparer implements Comparator<Node> {

	public static Comparer comparer = new Comparer();

	private static Map<Class<? extends Node>, Integer> order = new HashMap<>();
	static {
		order.put(Reference.class, 0);
		order.put(Int.class, 10);
		order.put(Atom.class, 20);
		order.put(Str.class, 30);
		order.put(TreeAnd.class, 40);
		order.put(TreeOp.class, 40);
		order.put(TreeOr.class, 40);
		order.put(TreeTuple.class, 40);
		order.put(Tuple.class, 50);
		order.put(Dict.class, 60);
	}

	@Override
	public int compare(Node n0, Node n1) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();
		var clazz0 = n0.getClass();
		var clazz1 = n1.getClass();
		int c = Integer.compare(order.get(clazz0), order.get(clazz1));

		if (c == 0)
			if (clazz0 == Atom.class)
				return Atom.name(n0).compareTo(Atom.name(n1));
			else if (clazz0 == Dict.class) {
				var m0 = Dict.m(n0);
				var m1 = Dict.m(n1);
				var keys = new HashSet<Node>();
				keys.addAll(m0.keySet());
				keys.addAll(m1.keySet());
				for (var key : Read.from(keys).sort(this::compare))
					c = c != 0 ? c : Compare.objects(m0.get(key), m1.get(key));
				return c;
			} else if (clazz0 == Int.class)
				return Integer.compare(Int.num(n0), Int.num(n1));
			else if (clazz0 == Reference.class)
				return Integer.compare(((Reference) n0).getId(), ((Reference) n1).getId());
			else if (clazz0 == Str.class)
				return Str.str(n0).compareTo(Str.str(n1));
			else if (Tree.class.isAssignableFrom(clazz0)) {
				var t0 = (Tree) n0;
				var t1 = (Tree) n1;
				c = t0.getOperator().precedence() - t1.getOperator().precedence();
				c = c != 0 ? c : compare(t0.getLeft(), t1.getLeft());
				c = c != 0 ? c : compare(t0.getRight(), t1.getRight());
				return c;
			} else if (clazz0 == Tuple.class) {
				var nodes0 = Tuple.t(n0);
				var nodes1 = Tuple.t(n1);
				int i = 0, l = min(nodes0.length, nodes1.length);
				while (c == 0 && i < l)
					c = compare(nodes0[i], nodes1[i]);
				if (c == 0)
					c = Integer.compare(nodes0.length, nodes1.length);
				return c;

			} else
				return Integer.compare(n0.hashCode(), n1.hashCode());
		else
			return c;
	}

}
