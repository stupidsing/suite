package suite.lp.kb;

import suite.lp.doer.TermParser.TermOp;
import suite.lp.node.Atom;
import suite.lp.node.Node;
import suite.lp.node.Tree;

public class Rule {

	private Node head, tail;

	public Rule(Node head, Node tail) {
		this.head = head;
		this.tail = tail;
	}

	public static Rule formRule(Node node) {
		Tree tree = Tree.decompose(node, TermOp.IS____);
		if (tree != null)
			return new Rule(tree.getLeft(), tree.getRight());
		else
			return new Rule(node, Atom.NIL);
	}

	public static Node formClause(Rule rule) {
		Node head = rule.getHead(), tail = rule.getTail();
		if (tail != Atom.NIL)
			return Tree.create(TermOp.IS____, head, tail);
		else
			return head;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + head.hashCode();
		result = 31 * result + tail.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Rule) {
			Rule other = (Rule) object;
			return head.equals(other.head) && tail.equals(other.tail);
		} else
			return false;
	}

	public Node getHead() {
		return head;
	}

	public Node getTail() {
		return tail;
	}

}
