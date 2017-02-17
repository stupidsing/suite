package suite.lp.kb;

import suite.lp.doer.ProverConstant;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class Rule {

	public final Node head, tail;
	private SewingGeneralizerImpl sewingGeneralizer;
	private int cutIndex;
	private Fun<Env, Node> headFun, tailFun;

	public Rule(Node head, Node tail) {
		this.head = head;
		this.tail = tail;
	}

	public static Rule of(Node node) {
		Tree tree = Tree.decompose(node, TermOp.IS____);
		if (tree != null)
			return new Rule(tree.getLeft(), tree.getRight());
		else
			return new Rule(node, Atom.NIL);
	}

	public synchronized Node newClause(Node query, Node cut) {
		if (sewingGeneralizer == null) {
			sewingGeneralizer = new SewingGeneralizerImpl();
			headFun = sewingGeneralizer.compile(head);
			tailFun = sewingGeneralizer.compile(tail);
			cutIndex = sewingGeneralizer.findVariableIndex(ProverConstant.cut);
		}

		Env env = sewingGeneralizer.env();
		env.getReference(cutIndex).bound(cut);

		return Tree.of(TermOp.AND___ //
				,
				Tree.of(TermOp.EQUAL_ //
						, query //
						, headFun.apply(env)) //
				, tailFun.apply(env));

	}

	public Node clause() {
		if (tail != Atom.NIL)
			return Tree.of(TermOp.IS____, head, tail);
		else
			return head;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Rule.class) {
			Rule other = (Rule) object;
			return head.equals(other.head) && tail.equals(other.tail);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + head.hashCode();
		result = 31 * result + tail.hashCode();
		return result;
	}

}
