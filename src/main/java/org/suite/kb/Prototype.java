package org.suite.kb;

import org.parser.Operator;
import org.suite.doer.Generalizer;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.util.Util;

public class Prototype {

	private Operator operator;
	private Node head;

	private static Generalizer generalizer = new Generalizer();

	public Prototype(Operator operator, Node head) {
		this.operator = operator;
		this.head = head;
	}

	public static Prototype get(Rule rule) {
		return get(rule.getHead());
	}

	public static Prototype get(Node head) {
		Operator operator = null;
		Tree t0 = null, t1;
		while ((t1 = Tree.decompose(head)) != null) {
			t0 = t1;
			operator = t0.getOperator();
			head = t0.getLeft();
		}

		if (!generalizer.isVariant(head) && !(head instanceof Reference))
			return new Prototype(operator, head);
		else
			return null;
	}

	@Override
	public int hashCode() {
		return Util.hashCode(operator) ^ Util.hashCode(head);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Prototype) {
			Prototype p = (Prototype) object;
			return Util.equals(operator, p.operator)
					&& Util.equals(head, p.head);
		} else
			return false;
	}

}
