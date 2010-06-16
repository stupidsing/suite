package org.suite;

import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;
import org.util.Util;

public class Binder {

	public static boolean bind(Node n1, Node n2, Journal journal) {
		n1 = n1.finalNode();
		n2 = n2.finalNode();

		if (n1 instanceof Reference) {
			journal.addBind((Reference) n1, n2);
			return true;
		} else if (n2 instanceof Reference) {
			journal.addBind((Reference) n2, n1);
			return true;
		}

		if (n1 == n2)
			return true;

		Class<? extends Node> clazz1 = n1.getClass();
		Class<? extends Node> clazz2 = n2.getClass();

		if (clazz1 != clazz2)
			return false;
		else if (clazz1 == Int.class)
			return ((Int) n1).getNumber() == ((Int) n2).getNumber();
		else if (clazz1 == Str.class)
			return Util.equals(((Str) n1).getValue(), ((Str) n2).getValue());
		else if (clazz1 == Tree.class) {
			Tree t1 = (Tree) n1;
			Tree t2 = (Tree) n2;
			return t1.getOperator() == t2.getOperator()
					&& bind(t1.getLeft(), t2.getLeft(), journal)
					&& bind(t1.getRight(), t2.getRight(), journal);
		} else
			return false;
	}

}
