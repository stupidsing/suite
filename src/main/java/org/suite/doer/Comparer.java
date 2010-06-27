package org.suite.doer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;

public class Comparer implements Comparator<Node> {

	public final static Comparer comparer = new Comparer();

	private static Map<Class<? extends Node>, Integer> order = new HashMap<Class<? extends Node>, Integer>();
	static {
		order.put(Reference.class, 0);
		order.put(Int.class, 10);
		order.put(Atom.class, 20);
		order.put(Str.class, 30);
		order.put(Tree.class, 40);
	}

	@Override
	public int compare(Node n1, Node n2) {
		n1 = n1.finalNode();
		n2 = n2.finalNode();

		Class<? extends Node> clazz1 = n1.getClass();
		Class<? extends Node> clazz2 = n2.getClass();
		if (clazz1 == clazz2)
			if (clazz1 == Atom.class)
				return ((Atom) n1).getName().compareTo(((Atom) n2).getName());
			else if (clazz1 == Int.class)
				return ((Int) n1).getNumber() - ((Int) n2).getNumber();
			else if (clazz1 == Str.class)
				return ((Str) n1).getValue().compareTo(((Str) n2).getValue());
			else if (clazz1 == Tree.class) {
				Tree t1 = (Tree) n1;
				Tree t2 = (Tree) n2;
				int c = t1.getOperator().getPrecedence()
						- t2.getOperator().getPrecedence();
				if (c == 0) {
					c = compare(t1.getLeft(), t2.getLeft());
					if (c == 0)
						c = compare(t1.getRight(), t2.getRight());
				}

				return c;
			} else
				return n1.hashCode() - n2.hashCode();
		else
			return order.get(clazz1) - order.get(clazz2);
	}

}
