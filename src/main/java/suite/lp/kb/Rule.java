package suite.lp.kb;

import primal.Verbs.Get;
import suite.lp.doer.GeneralizerFactory.Generalize_;
import suite.lp.doer.ProverConstant;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.BaseOp;
import suite.node.io.TermOp;

public class Rule {

	public final Node head, tail;
	private SewingGeneralizerImpl sewingGeneralizer;
	private int cutIndex;
	private Generalize_ headFun, tailFun;

	public Rule(Node head, Node tail) {
		this.head = head;
		this.tail = tail;
	}

	public static Rule of(Node node) {
		var tree = Tree.decompose(node, TermOp.IS____);
		if (tree != null)
			return new Rule(tree.getLeft(), tree.getRight());
		else
			return new Rule(node, Atom.NIL);
	}

	public synchronized Node newClause(Node query, Node cut) {
		if (sewingGeneralizer == null) {
			sewingGeneralizer = new SewingGeneralizerImpl(ProverConstant.variablePrefix);
			headFun = sewingGeneralizer.generalizer(head);
			tailFun = sewingGeneralizer.generalizer(tail);
			cutIndex = sewingGeneralizer.mapper().computeIndex(ProverConstant.cut);
		}

		var env = sewingGeneralizer.mapper().env();
		env.refs[cutIndex].bound(cut);

		return Tree.ofAnd(//
				Tree.of(BaseOp.EQUAL_, //
						query, //
						headFun.apply(env)), //
				tailFun.apply(env));

	}

	public Node clause() {
		return tail != Atom.NIL ? Tree.of(TermOp.IS____, head, tail) : head;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == Rule.class) {
			var other = (Rule) object;
			return head.equals(other.head) && tail.equals(other.tail);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		var h = 7;
		h = h * 31 + head.hashCode();
		h = h * 31 + tail.hashCode();
		return h;
	}

}
