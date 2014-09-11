package suite.lp.kb;

import java.util.Objects;

import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.util.Util;

/**
 * Index rules by the first atom in their heads.
 *
 * @author ywsing
 */
public class Prototype implements Comparable<Prototype> {

	private Node head;

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

		boolean indexable = node != null && !Generalizer.isVariant(node) && !(node instanceof Reference);
		return indexable ? new Prototype(node) : null;
	}

	public Prototype(Node head) {
		this.head = head;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(head);
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Prototype.class) {
			Prototype p = (Prototype) object;
			return Objects.equals(head, p.head);
		} else
			return false;
	}

	@Override
	public String toString() {
		return head.toString();
	}

	public Node getHead() {
		return head;
	}

	@Override
	public int compareTo(Prototype other) {
		return head.compareTo(other.head);
	}

}
