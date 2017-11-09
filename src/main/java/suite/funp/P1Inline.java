package suite.funp;

import java.util.HashMap;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpReference;
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
		Funp node1 = inlineDefine(node0);
		Funp node2 = inlineLambda(node1);
		return node2;
	}

	private Funp inlineDefine(Funp node) {
		Map<FunpVariable, Funp> defByVariables = new HashMap<>();

		new Object() {
			private Funp associate(IMap<String, Funp> vars, Funp node_) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
							associate(vars, value);
							associate(vars.replace(var, f), expr);
							return n_;
						})) //
						.applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
							IMap<String, Funp> vars1 = vars;
							for (Pair<String, Funp> pair : pairs)
								vars1 = vars1.replace(pair.t0, f);
							for (Pair<String, Funp> pair : pairs)
								associate(vars1, pair.t1);
							associate(vars1, expr);
							return n_;
						})) //
						.applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
							associate(vars.replace(var, f), expr);
							return n_;
						})) //
						.applyIf(FunpVariable.class, f -> f.apply(var -> {
							defByVariables.put(f, vars.get(var));
							return n_;
						})) //
						.result(), node_);
			}
		}.associate(IMap.empty(), node);

		Map<Funp, IntMutable> countByDefs = new HashMap<>();

		inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
				.applyIf(FunpReference.class, f -> f.apply(expr -> {
					countByDefs.computeIfAbsent(defByVariables.get(expr), v -> IntMutable.of(0)).update(9999);
					return null;
				})) //
				.applyIf(FunpVariable.class, f -> f.apply(var -> {
					countByDefs.computeIfAbsent(defByVariables.get(f), v -> IntMutable.of(0)).increment();
					return null;
				})) //
				.result(), node);

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

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(Funp.class, n_ -> {
					FunpDefine define;
					if ((define = defines.get(n_)) != null)
						return inline(define.expr);
					else if ((define = expands.get(n_)) != null)
						return inline(define.value);
					else
						return null;
				}, node_);
			}
		}.inline(node);
	}

	private Funp inlineLambda(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
							if (lambda instanceof FunpLambda) {
								FunpLambda lambda1 = (FunpLambda) lambda;
								return FunpDefine.of(lambda1.var, inline(value), inline(lambda1.expr));
							} else
								return null;
						})) //
						.result(), node_);
			}
		}.inline(node);
	}

}
