package suite.funp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpAssignReference;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.primitive.IntMutable;
import suite.streamlet.Read;
import suite.util.List_;
import suite.util.String_;
import suite.util.Switch;

public class P1Inline {

	private Inspect inspect = Singleton.me.inspect;

	public Funp inline(Funp node) {
		for (int i = 0; i < 3; i++) {
			if (Boolean.FALSE)
				node = inlineDefineAssign(node);
			node = inlineDefine(node);
			node = inlineLambda(node);
		}
		return node;
	}

	private Funp inlineDefineAssign(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(Funp.class, n0 -> {
					List<String> vars = new ArrayList<>();
					FunpAssignReference assign;
					FunpDefine define;
					Funp ref, var;

					while (n0 instanceof FunpDefine //
							&& (define = (FunpDefine) n0).value instanceof FunpDontCare) {
						vars.add(define.var);
						n0 = define.expr;
					}

					if (n0 instanceof FunpAssignReference //
							&& (ref = (assign = (FunpAssignReference) n0).reference) instanceof FunpReference //
							&& (var = ((FunpReference) ref).expr) instanceof FunpVariable) {
						String vn = ((FunpVariable) var).var;
						Funp n1 = assign.expr;
						boolean b = false;

						for (String var_ : List_.reverse(vars))
							if (!String_.equals(vn, var_))
								n1 = FunpDefine.of(var_, FunpDontCare.of(), n1);
							else
								b = true;

						if (b)
							return FunpDefine.of(vn, assign.value, inline(n1));
					}

					return null;
				}, node_);
			}
		}.inline(node);
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
