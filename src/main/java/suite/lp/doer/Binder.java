package suite.lp.doer;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import suite.lp.Journal;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;

public class Binder {

	public static boolean bind(Node n0, Node n1, Journal journal) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();

		if (n0 == n1)
			return true;

		if (n0 instanceof Reference) {
			journal.addBind((Reference) n0, n1);
			return true;
		} else if (n1 instanceof Reference) {
			journal.addBind((Reference) n1, n0);
			return true;
		}

		Class<? extends Node> clazz0 = n0.getClass();
		Class<? extends Node> clazz1 = n1.getClass();

		if (clazz0 != clazz1)
			return false;
		else if (clazz0 == Int.class)
			return ((Int) n0).number == ((Int) n1).number;
		else if (clazz0 == Str.class)
			return Objects.equals(((Str) n0).value, ((Str) n1).value);
		else if (clazz0 == Tree.class) {
			Tree t0 = (Tree) n0;
			Tree t1 = (Tree) n1;
			return t0.getOperator() == t1.getOperator() //
					&& bind(t0.getLeft(), t1.getLeft(), journal) //
					&& bind(t0.getRight(), t1.getRight(), journal);
		} else if (clazz0 == Tuple.class) {
			List<Node> nodes0 = ((Tuple) n0).nodes;
			List<Node> nodes1 = ((Tuple) n1).nodes;
			boolean result = nodes0.size() == nodes1.size();
			if (result) {
				Iterator<Node> iter0 = nodes0.iterator();
				Iterator<Node> iter1 = nodes1.iterator();
				while (result && iter0.hasNext())
					result &= bind(iter0.next(), iter1.next(), journal);
			}
			return result;
		} else
			return false;
	}

}
