package suite.funp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpAssignReference;
import suite.funp.P0.FunpCheckType;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
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
			node = inlineDefineAssigns(node);
			Map<FunpVariable, Funp> defByVariables = associateDefinitions(node);
			node = inlineDefines(node, defByVariables);
			if (Boolean.FALSE)
				node = inlineFields(node, defByVariables);
			node = inlineLambdas(node);
		}
		return node;
	}

	private Funp inlineDefineAssigns(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(Funp.class, n0 -> {
					List<String> vars = new ArrayList<>();
					FunpAssignReference assign;
					FunpCheckType check;
					FunpDefine define;
					FunpVariable variable;

					while ((define = n0.cast(FunpDefine.class)) != null //
							&& define.value instanceof FunpDontCare //
							&& !define.isPolyType) {
						vars.add(define.var);
						n0 = define.expr;
					}

					if ((check = n0.cast(FunpCheckType.class)) != null)
						n0 = check.expr;

					if ((assign = n0.cast(FunpAssignReference.class)) != null //
							&& (variable = assign.reference.expr.cast(FunpVariable.class)) != null) {
						String vn = variable.var;
						Funp n1 = assign.expr;
						Funp n2 = check != null ? FunpCheckType.of(check.left, check.right, n1) : n1;
						boolean b = false;

						for (String var_ : List_.reverse(vars))
							if (!String_.equals(vn, var_))
								n2 = FunpDefine.of(false, var_, FunpDontCare.of(), n2);
							else
								b = true;

						if (b)
							return FunpDefine.of(false, vn, assign.value, inline(n2));
					}

					return null;
				}, node_);
			}
		}.inline(node);
	}

	private Funp inlineDefines(Funp node, Map<FunpVariable, Funp> defByVariables) {
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
						return define.value;
					else
						return null;
				}, node_);
			}
		}.inline(node);
	}

	private Funp inlineFields(Funp node, Map<FunpVariable, Funp> defs) {
		return new Object() {
			private Funp inline(Funp node_) {
				FunpField field;
				FunpStruct struct;
				FunpVariable variable;
				if ((field = node_.cast(FunpField.class)) != null //
						&& (variable = field.reference.cast(FunpReference.class, n -> n.expr).cast(FunpVariable.class)) != null //
						&& (struct = defs.get(variable).cast(FunpDefine.class, n -> n.value).cast(FunpStruct.class)) != null)
					return Read.from2(struct.pairs).filterKey(field_ -> String_.equals(field_, field.field)).uniqueResult().t1;
				else
					return null;
			}
		}.inline(node);
	}

	private Funp inlineLambdas(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
							return lambda.cast(FunpLambda.class, n -> FunpDefine.of(false, n.var, inline(value), inline(n.expr)));
						})) //
						.result(), node_);
			}
		}.inline(node);
	}

	private Map<FunpVariable, Funp> associateDefinitions(Funp node) {
		Map<FunpVariable, Funp> defByVariables = new HashMap<>();

		new Object() {
			private Funp associate(IMap<String, Funp> vars, Funp node_) {
				return inspect.rewrite(Funp.class, n_ -> new Switch<Funp>(n_) //
						.applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
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

		return defByVariables;
	}

}
