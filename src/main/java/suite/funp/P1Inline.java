package suite.funp;

import java.util.HashMap;
import java.util.Map;

import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.primitive.IntMutable;
import suite.util.Switch;

public class P1Inline {

	private Inspect inspect = Singleton.me.inspect;

	public Funp inline(Funp node0) {
		Map<FunpDefine, IntMutable> counts = new HashMap<>();

		new Object() {
			private Funp count(IMap<String, FunpDefine> vars, Funp node) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
							counts.put(f, IntMutable.of(0));
							count(vars, value);
							count(vars.put(var, f), expr);
							return null;
						})) //
						.applyIf(FunpVariable.class, f -> f.apply(var -> {
							FunpDefine def = vars.get(((FunpVariable) n_).var);
							if (def != null)
								counts.get(def).increment();
							return null;
						})) //
						.result(), node);
			}
		}.count(IMap.empty(), node0);

		Funp node1 = new Object() {
			private Funp expand(IMap<String, FunpDefine> vars, Funp node) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
							IMap<String, FunpDefine> vars1 = vars.put(var, f);
							return 1 < counts.get(n_).get() //
									? FunpDefine.of(var, expand(vars, value), expand(vars1, expr)) //
									: expand(vars1, expr);
						})) //
						.applyIf(FunpVariable.class, f -> f.apply(var -> {
							FunpDefine def = vars.get(var);
							if (def != null && counts.get(def).get() == 1)
								return def.value;
							else
								return n_;
						})) //
						.result(), node);
			}
		}.expand(IMap.empty(), node0);

		return new Object() {
			private Funp expand(Funp node) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
							if (lambda instanceof FunpLambda) {
								FunpLambda lambda1 = (FunpLambda) lambda;
								return FunpDefine.of(lambda1.var, expand(value), expand(lambda1.expr));
							} else
								return null;
						})) //
						.result(), node);
			}
		}.expand(node1);
	}

}
