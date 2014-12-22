package suite.lp.sewing;

import suite.Suite;
import suite.lp.sewing.VariableMapping.Env;
import suite.node.Int;
import suite.node.Node;

public class SewingExpression {

	private SewingGeneralizer sg;

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public SewingExpression(SewingGeneralizer sg) {
		this.sg = sg;
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
			int index = sg.findVariableIndex(node);
			return env -> ((Int) env.refs[index].finalNode()).number;
		}
	}

}
