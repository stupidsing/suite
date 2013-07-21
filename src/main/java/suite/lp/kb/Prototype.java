package suite.lp.kb;

import suite.lp.doer.Generalizer;
import suite.lp.node.Atom;
import suite.lp.node.Node;
import suite.lp.node.Reference;
import suite.lp.node.Tree;
import suite.util.Util;

/**
 * Index rules by the first atom in their heads.
 * 
 * @author ywsing
 */
public class Prototype {

	private Node head;

	private static Generalizer generalizer = new Generalizer();

	public Prototype(Node head) {
		this.head = head;
	}

	public static Prototype get(Rule rule) {
		return get(rule, 0);
	}

	public static Prototype get(Rule rule, int n) {
		return get(rule.getHead(), n);
	}

	public static Prototype get(Node node) {
		return get(node, 0);
	}

	public static Prototype get(Node node, int n) {
		for (int i = 0; i < n; i++) {
			Tree tree = Tree.decompose(node);
			node = tree != null ? tree.getRight() : Atom.NIL;
		}

		if (node != null) {
			Tree t0, t1;

			while ((t1 = Tree.decompose(node)) != null) {
				t0 = t1;
				node = t0.getLeft();
			}
		}

		boolean indexable = node != null && !generalizer.isVariant(node) && !(node instanceof Reference);
		return indexable ? new Prototype(node) : null;
	}

	@Override
	public int hashCode() {
		return Util.hashCode(head);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Prototype) {
			Prototype p = (Prototype) object;
			return Util.equals(head, p.head);
		} else
			return false;
	}

	public Node getHead() {
		return head;
	}

}
