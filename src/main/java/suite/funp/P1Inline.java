package suite.funp;

import java.util.HashMap;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.primitive.IntMutable;
import suite.streamlet.Read;
import suite.util.Switch;

public class P1Inline {

	private Inspect inspect = Singleton.me.inspect;

	public Funp inline(Funp node0) {
		Map<FunpVariable, Funp> defByVariables = new HashMap<>();

		new Object() {
			private Funp associate(IMap<String, Funp> vars, Funp node) {
				return inspect.rewrite(Funp.class, node_ -> new Switch<Funp>(node_) //
						.applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
							associate(vars, value);
							associate(vars.put(var, f), expr);
							return node_;
						})) //
						.applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
							IMap<String, Funp> vars1 = vars;
							for (Pair<String, Funp> pair : pairs)
								vars1 = vars1.put(pair.t0, f);
							for (Pair<String, Funp> pair : pairs)
								associate(vars1, pair.t1);
							associate(vars1, expr);
							return node_;
						})) //
						.applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
							associate(vars.put(var, f), expr);
							return node_;
						})) //
						.applyIf(FunpVariable.class, f -> f.apply(var -> {
							defByVariables.put(f, vars.get(var));
							return node_;
						})) //
						.result(), node);
			}
		}.associate(IMap.empty(), node0);

		Map<Funp, IntMutable> countByDefs = new HashMap<>();

		new Object() {
			private void count(Funp node) {
				inspect.rewrite(Funp.class, node_ -> {
					if (node_ instanceof FunpVariable)
						countByDefs.computeIfAbsent(defByVariables.get((FunpVariable) node_), v -> IntMutable.of(0)).increment();
					return null;
				}, node);
			}
		}.count(node0);

		Map<Funp, FunpDefine> defines = Read //
				.from2(defByVariables) //
				.values() //
				.filter(def -> def instanceof FunpDefine && countByDefs.get(def).get() <= 1) //
				.map2(def -> (FunpDefine) def) //
				.toMap();

		Map<FunpVariable, FunpDefine> expands = Read //
				.from2(defByVariables) //
				.mapValue(defines::get) //
				.filterValue(def -> def != null) //
				.toMap();

		Funp node1 = new Object() {
			private Funp expand(Funp node) {
				return inspect.rewrite(Funp.class, node_ -> {
					FunpDefine define;
					if ((define = defines.get(node_)) != null)
						return expand(define.expr);
					else if ((define = expands.get(node_)) != null)
						return expand(define.value);
					else
						return null;
				}, node);
			}
		}.expand(node0);

		return new Object() {
			private Funp expand(Funp node) {
				return inspect.rewrite(Funp.class, node_ -> new Switch<Funp>(node_) //
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
