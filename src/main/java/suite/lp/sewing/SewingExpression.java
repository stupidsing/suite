package suite.lp.sewing;

import suite.Suite;
import suite.lp.sewing.VariableMapper.Env;
import suite.node.Int;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class SewingExpression {

	private SewingCloner sc;

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public SewingExpression(SewingCloner sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		node = node.finalNode();
		Node m[];

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
			Fun<Env, Node> f = sc.compile(node);
			return env -> ((Int) f.apply(env).finalNode()).number;
		}
	}

}
