package suite.lp.sewing.impl;

import suite.lp.doer.ClonerFactory;
import suite.lp.doer.ClonerFactory.Clone_;
import suite.lp.doer.EvaluatorFactory;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.IntInt_Int;

public class SewingExpressionImpl implements EvaluatorFactory {

	private ClonerFactory sc;

	public SewingExpressionImpl(ClonerFactory sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator op = tree.getOperator();
			Evaluate lhs, rhs;
			IntInt_Int fun;

			if (op == TermOp.TUPLE_) {
				Tree rightTree = Tree.decompose(tree.getRight());
				lhs = compile(tree.getLeft());
				rhs = compile(rightTree.getRight());
				fun = TreeUtil.evaluateOp(rightTree.getLeft());
			} else {
				lhs = compile(tree.getLeft());
				rhs = compile(tree.getRight());
				fun = TreeUtil.evaluateOp(op);
			}

			return env -> fun.apply(lhs.evaluate(env), rhs.evaluate(env));
		} else if (node instanceof Int) {
			int i = ((Int) node).number;
			return env -> i;
		} else {
			Clone_ f = sc.compile(node);
			return env -> TreeUtil.evaluate(f.apply(env));
		}
	}

}
