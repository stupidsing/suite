package suite.lp.sewing.impl;

import suite.Suite;
import suite.lp.predicate.EvalPredicates;
import suite.lp.sewing.SewingCloner;
import suite.lp.sewing.SewingCloner.Clone_;
import suite.lp.sewing.SewingExpression;
import suite.node.Int;
import suite.node.Node;

public class SewingExpressionImpl0 implements SewingExpression {

	private SewingCloner sc;

	public SewingExpressionImpl0(SewingCloner sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		Node[] m;

		if ((m = Suite.matcher(".0 + .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) + e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 - .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) - e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 * .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) * e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 / .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) / e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 and .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) & e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 or .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) | e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 shl .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) << e1.evaluate(env);
		} else if ((m = Suite.matcher(".0 shr .1").apply(node)) != null) {
			Evaluate e0 = compile(m[0]);
			Evaluate e1 = compile(m[1]);
			return env -> e0.evaluate(env) >> e1.evaluate(env);
		} else if (node instanceof Int) {
			int i = ((Int) node).number;
			return env -> i;
		} else {
			Clone_ f = sc.compile(node);
			return env -> new EvalPredicates().evaluate(f.apply(env));
		}
	}

}