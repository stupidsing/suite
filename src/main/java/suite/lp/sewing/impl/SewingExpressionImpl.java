package suite.lp.sewing.impl;

import suite.lp.doer.ClonerFactory;
import suite.lp.doer.EvaluatorFactory;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.node.util.TreeUtil;

public class SewingExpressionImpl implements EvaluatorFactory {

	private ClonerFactory clonerFactory;

	public SewingExpressionImpl(ClonerFactory clonerFactory) {
		this.clonerFactory = clonerFactory;
	}

	public Evaluate_ evaluator(Node node) {
		return new SwitchNode<Evaluate_>(node
		).match(".0 .1 .2", (l, op, r) -> {
			var lhs = evaluator(l);
			var rhs = evaluator(r);
			var fun = TreeUtil.evaluateOp(op);
			return env -> fun.apply(lhs.evaluate(env), rhs.evaluate(env));
		}).applyTree((op, l, r) -> {
			var lhs = evaluator(l);
			var rhs = evaluator(r);
			var fun = TreeUtil.evaluateOp(op);
			return env -> fun.apply(lhs.evaluate(env), rhs.evaluate(env));
		}).applyIf(Int.class, i -> {
			var i_ = i.number;
			return env -> i_;
		}).applyIf(Node.class, n -> {
			var f = clonerFactory.cloner(node);
			return env -> TreeUtil.evaluate(f.apply(env));
		}).nonNullResult();
	}

}
