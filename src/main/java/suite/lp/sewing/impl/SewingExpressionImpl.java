package suite.lp.sewing.impl;

import suite.lp.doer.ClonerFactory;
import suite.lp.doer.EvaluatorFactory;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.IntInt_Int;

public class SewingExpressionImpl implements EvaluatorFactory {

	private ClonerFactory clonerFactory;

	public SewingExpressionImpl(ClonerFactory clonerFactory) {
		this.clonerFactory = clonerFactory;
	}

	public Evaluate_ evaluator(Node node) {
		var tree = Tree.decompose(node);

		if (tree != null) {
			var op = tree.getOperator();
			Evaluate_ lhs, rhs;
			IntInt_Int fun;

			if (op == TermOp.TUPLE_) {
				var rightTree = Tree.decompose(tree.getRight());
				lhs = evaluator(tree.getLeft());
				rhs = evaluator(rightTree.getRight());
				fun = TreeUtil.evaluateOp(rightTree.getLeft());
			} else {
				lhs = evaluator(tree.getLeft());
				rhs = evaluator(tree.getRight());
				fun = TreeUtil.evaluateOp(op);
			}

			return env -> fun.apply(lhs.evaluate(env), rhs.evaluate(env));
		} else if (node instanceof Int) {
			var i = ((Int) node).number;
			return env -> i;
		} else {
			var f = clonerFactory.cloner(node);
			return env -> TreeUtil.evaluate(f.apply(env));
		}
	}

}
