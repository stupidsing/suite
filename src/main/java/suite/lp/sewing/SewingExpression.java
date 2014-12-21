package suite.lp.sewing;

import suite.Suite;
import suite.lp.sewing.VariableMapping.Env;
import suite.node.Int;
import suite.node.Node;

public class SewingExpression {

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public Evaluate compile(SewingGeneralizer sg, Node node) {
		node = node.finalNode();
		Node m[];

		if ((m = Suite.match(".0 + .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) + e1.evaluate(env);
		} else if ((m = Suite.match(".0 - .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) - e1.evaluate(env);
		} else if ((m = Suite.match(".0 * .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) - e1.evaluate(env);
		} else if ((m = Suite.match(".0 / .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) - e1.evaluate(env);
		} else if ((m = Suite.match(".0 and .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) & e1.evaluate(env);
		} else if ((m = Suite.match(".0 or .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) | e1.evaluate(env);
		} else if ((m = Suite.match(".0 shl .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) << e1.evaluate(env);
		} else if ((m = Suite.match(".0 shr .1", node)) != null) {
			Evaluate e0 = compile(sg, m[0]);
			Evaluate e1 = compile(sg, m[1]);
			return env -> e0.evaluate(env) >> e1.evaluate(env);
		} else if (node instanceof Int) {
			int i = ((Int) node).number;
			return env -> i;
		} else {
			int index = sg.getVariableIndex(node);
			return env -> ((Int) env.refs[index].finalNode()).number;
		}
	}

}
