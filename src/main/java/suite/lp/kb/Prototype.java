package suite.lp.kb;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import suite.adt.map.ListMultimap;
import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.tree.TreeTuple;
import suite.streamlet.Read;
import suite.util.Object_;

/**
 * Index rules by the first atom in their heads.
 *
 * @author ywsing
 */
public class Prototype implements Comparable<Prototype> {

	public final Node head;

	public static ListMultimap<Prototype, Rule> multimap(RuleSet ruleSet) {
		return multimap(ruleSet.getRules());
	}

	public static ListMultimap<Prototype, Rule> multimap(List<Rule> rules) {
		return Read.from(rules).toMultimap(Prototype::of);
	}

	public static Prototype of(Rule rule) {
		return of(rule.head);
	}

	public static Prototype of(Rule rule, int n) {
		return of(rule.head, n);
	}

	public static Prototype of(Node node, int n) {
		for (var i = 0; i < n; i++) {
			var tree = decompose(node);
			node = tree != null ? tree.getRight() : Atom.NIL;
		}

		return node != null ? Prototype.of(node) : null;
	}

	public static Prototype of(Node node) {
		Tree t0, t1;

		while ((t1 = decompose(node)) != null) {
			t0 = t1;
			node = t0.getLeft();
		}

		var indexable = !ProverConstant.isVariant(node) && !(node instanceof Reference);
		return indexable ? new Prototype(node) : null;
	}

	private static Tree decompose(Node node) {
		Tree tree;
		if ((tree = Tree.decompose(node)) != null)
			return tree;
		else if (node instanceof Tuple) {
			var nodes = ((Tuple) node).nodes;
			if (nodes.length > 0)
				return TreeTuple.of(nodes[0], Tuple.of(Arrays.copyOfRange(nodes, 1, nodes.length, Node[].class)));
			else
				return null;
		} else
			return null;
	}

	private Prototype(Node head) {
		this.head = head;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Prototype.class) {
			var p = (Prototype) object;
			return Objects.equals(head, p.head);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(head);
	}

	@Override
	public String toString() {
		return head.toString();
	}

	@Override
	public int compareTo(Prototype other) {
		return head.compareTo(other.head);
	}

}
