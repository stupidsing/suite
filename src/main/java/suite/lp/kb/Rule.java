package suite.lp.kb;

import suite.lp.sewing.SewingGeneralizer;
import suite.lp.sewing.SewingGeneralizer.Env;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class Rule {

	private Node head, tail;
	private SewingGeneralizer sewingGeneralizer;
	private Fun<Env, Node> headFun, tailFun;

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
			return Tree.of(TermOp.IS____, head, tail);
		else
			return head;
	}

	public synchronized Node createClause(Node query, Node cut) {
		if (sewingGeneralizer == null) {
			sewingGeneralizer = new SewingGeneralizer();
			headFun = sewingGeneralizer.compile(head);
			tailFun = sewingGeneralizer.compile(tail);
		}

		Env env = sewingGeneralizer.env(cut);

		return Tree.of(TermOp.AND___ //
				, Tree.of(TermOp.EQUAL_ //
						, query //
						, headFun.apply(env)) //
				, tailFun.apply(env));

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
		if (Util.clazz(object) == Rule.class) {
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
