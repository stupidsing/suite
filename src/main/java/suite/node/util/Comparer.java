package suite.node.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.streamlet.Read;
import suite.util.Util;

public class Comparer implements Comparator<Node> {

	public static Comparer comparer = new Comparer();

	private static Map<Class<? extends Node>, Integer> order = new HashMap<>();
	static {
		order.put(Reference.class, 0);
		order.put(Int.class, 10);
		order.put(Atom.class, 20);
		order.put(Str.class, 30);
		order.put(Tree.class, 40);
		order.put(Tuple.class, 50);
		order.put(Dict.class, 60);
	}

	@Override
	public int compare(Node n0, Node n1) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();
		Class<? extends Node> clazz0 = n0.getClass();
		Class<? extends Node> clazz1 = n1.getClass();

		if (clazz0 == clazz1)
			if (clazz0 == Atom.class)
				return ((Atom) n0).name.compareTo(((Atom) n1).name);
			else if (clazz0 == Dict.class) {
				Map<Node, Node> m0 = ((Dict) n0).map;
				Map<Node, Node> m1 = ((Dict) n1).map;
				Set<Node> keys = new HashSet<>();
				keys.addAll(m0.keySet());
				keys.addAll(m1.keySet());
				int c = 0;
				for (Node key : Read.from(keys).sort(this::compare).toList()) {
					Node v0 = m0.get(key);
					Node v1 = m1.get(key);
					if ((c = Util.compare(v0, v1)) != 0)
						break;
				}
				return c;
			} else if (clazz0 == Int.class)
				return ((Int) n0).number - ((Int) n1).number;
			else if (clazz0 == Reference.class)
				return ((Reference) n0).getId() - ((Reference) n1).getId();
			else if (clazz0 == Str.class)
				return ((Str) n0).value.compareTo(((Str) n1).value);
			else if (clazz0 == Tree.class) {
				Tree t0 = (Tree) n0;
				Tree t1 = (Tree) n1;
				int c = t0.getOperator().getPrecedence() - t1.getOperator().getPrecedence();
				c = c != 0 ? c : compare(t0.getLeft(), t1.getLeft());
				c = c != 0 ? c : compare(t0.getRight(), t1.getRight());
				return c;
			} else if (clazz0 == Tuple.class) {
				Iterator<Node> iter0 = ((Tuple) n0).nodes.iterator();
				Iterator<Node> iter1 = ((Tuple) n1).nodes.iterator();
				int c = 0;
				while (c == 0 && iter0.hasNext() && iter1.hasNext())
					c = compare(iter0.next(), iter1.next());
				if (c == 0)
					c = iter0.hasNext() ? 1 : -1;
				return c;

			} else
				return n0.hashCode() - n1.hashCode();
		else
			return order.get(clazz0) - order.get(clazz1);
	}

}
