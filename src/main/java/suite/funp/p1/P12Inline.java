package suite.funp.p1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.Verbs.Reverse;
import primal.adt.Pair;
import primal.fp.Funs.Iterate;
import primal.persistent.PerMap;
import primal.primitive.adt.IntMutable;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fct;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpLambdaFree;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTag;
import suite.funp.P0.FunpTagId;
import suite.funp.P0.FunpTagValue;
import suite.funp.P0.FunpTypeCheck;
import suite.funp.P0.FunpVariable;
import suite.inspect.Dump;
import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class P12Inline {

	private Inspect inspect = Singleton.me.inspect;

	public P12Inline(Funp_ f) {
	}

	public Funp inline(Funp node, int rounds) {
		node = renameVariables(node);

		for (var i = 0; i < rounds; i++) {
			node = inlineDefineAssigns(node);
			node = inlineFields(node); // may reorder or duplicate calculations
			node = inlineLambdas(node);
			node = inlineTags(node); // may reorder or duplicate calculations
		}

		for (var i = 0; i < rounds; i++)
			node = inlineDefines(node); // may remove or reorder calculations

		if (Boolean.FALSE)
			Dump.line(node);

		return node;
	}

	// Before - v => v => v
	// After - v => v$1 => v$1
	private Funp renameVariables(Funp node) {
		var vns = new HashSet<>();

		Iterate<String> newVarName = vn -> {
			var vn1 = vn.split("\\$")[0];
			var i = 0;
			while (!vns.add(vn1))
				vn1 = vn + "$" + i++;
			return vn1;
		};

		class Rename {
			private PerMap<String, String> vns;

			private Rename(PerMap<String, String> vns) {
				this.vns = vns;
			}

			private Funp rename(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
					var vn_ = newVarName.apply(vn);
					var r1 = new Rename(vns.replace(vn, vn_));
					return FunpDefine.of(vn_, rename(value), r1.rename(expr), fdt);
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs0, expr, fdt) -> {
					var pairs = Read.from2(pairs0);
					var vns1 = pairs.keys().fold(vns, (vns_, vn) -> vns_.replace(vn, newVarName.apply(vn)));
					var r1 = new Rename(vns1);
					return FunpDefineRec.of( //
							pairs.map2((vn, value) -> vns1.getOrFail(vn), (vn, value) -> r1.rename(value)).toList(), //
							r1.rename(expr), //
							fdt);
				})).applyIf(FunpLambda.class, f -> f.apply((vn0, expr, fct) -> {
					var vn1 = newVarName.apply(vn0);
					var r1 = new Rename(vns.replace(vn0, vn1));
					return FunpLambda.of(vn1, r1.rename(expr), fct);
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return FunpVariable.of(vns.getOrFail(var));
				})).result());
			}
		}

		return new Rename(PerMap.empty()).rename(node);
	}

	// Before - define i := 1 ~ i + 1
	// After - 1 + 1
	private Funp inlineDefines(Funp node) {
		var defByVariables = Funp_.associateDefinitions(node);
		var countByDefs = new HashMap<Funp, IntMutable>();

		new Object() {
			public void count(Funp node_, boolean isWithinIo) {
				inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
					// too dangerous to inline imperative code
					getCount(f).update(isWithinIo ? 9999 : 0);
					return null;
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
					// too dangerous to inline imperative code
					getCount(f).update(isWithinIo ? 9999 : 0);
					return null;
				})).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
					getVariableCount(var).update(9999);
					return null;
				})).applyIf(FunpIo.class, f -> f.apply(expr -> {
					count(expr, true);
					return n_;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
					count(expr, false);
					return n_;
				})).applyIf(FunpLambdaFree.class, f -> f.apply((lambda, expr) -> {
					if (lambda instanceof FunpVariable fv)
						getVariableCount(fv).update(9999);
					return null;
				})).applyIf(FunpReference.class, f -> f.apply(expr -> {
					if (expr instanceof FunpVariable fv)
						getVariableCount(fv).update(9999);
					return null;
				})).applyIf(FunpTypeCheck.class, f -> f.apply((left, right, expr) -> {
					count(expr, isWithinIo);
					return n_;
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					getVariableCount(f).increment();
					return null;
				})).result());
			}

			private IntMutable getVariableCount(FunpVariable var) {
				return getCount(defByVariables.get(var));
			}

			private IntMutable getCount(Funp def) {
				return countByDefs.computeIfAbsent(def, v -> IntMutable.of(0));
			}
		}.count(node, false);

		var defines = Read //
				.from2(countByDefs) //
				.filter((def, c) -> def instanceof FunpDefine && c.value() <= 1) //
				.map2((def, c) -> def, (def, c) -> (FunpDefine) def) //
				.filterValue(def -> Fdt.isLocal(def.fdt) && Fdt.isPure(def.fdt)) //
				.toMap();

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					FunpDefine define;
					if ((define = defines.get(n_)) != null)
						return inline(define.expr);
					else if ((define = defines.get(defByVariables.get(n_))) != null)
						return inline(define.value);
					else
						return null;
				});
			}
		}.inline(node);
	}

	// Before - define i := don't_care ~ assign i := value ~ expr
	// After - define i := value ~ expr
	private Funp inlineDefineAssigns(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n0 -> {
					var vns = new ArrayList<Pair<String, Fdt>>();
					FunpTypeCheck check;

					while (n0 instanceof FunpDefine define //
							&& Fdt.isLocal(define.fdt) //
							&& define.value instanceof FunpDontCare) {
						vns.add(Pair.of(define.vn, define.fdt));
						n0 = define.expr;
					}

					if ((check = n0.cast(FunpTypeCheck.class)) != null)
						n0 = check.expr;

					if (n0 instanceof FunpDoAssignVar assign) {
						var vn = assign.var.vn;
						var n1 = assign.expr;
						var n2 = check != null ? FunpTypeCheck.of(check.left, check.right, n1) : n1;
						Fdt fdt = null;

						for (var pair : Reverse.of(vns)) {
							var vn_ = pair.k;
							var fdt_ = pair.v;
							if (!Equals.string(vn, vn_))
								n2 = FunpDefine.of(vn_, FunpDontCare.of(), n2, fdt_);
							else
								fdt = fdt_;
						}

						if (fdt != null)
							return FunpDefine.of(vn, assign.value, inline(n2), fdt);
					}

					return null;
				});
			}
		}.inline(node);
	}

	// Before - define s := (struct (a 1, b 2, c 3,)) ~ s/c
	// After - define s := (struct (a 1, b 2, c 3,)) ~ 3
	private Funp inlineFields(Funp node) {
		var defByVariables = Funp_.associateDefinitions(node);

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					if (n_ instanceof FunpField field //
							&& lookup(defByVariables, field.reference.expr) instanceof FunpStruct struct) {
						var pair = Read //
								.from2(struct.pairs) //
								.filterKey(field_ -> Equals.string(field_, field.field)) //
								.first();
						return pair != null ? inline(pair.v) : null;
					} else
						return null;
				});
			}
		}.inline(node);
	}

	// Before - 3 | (i => i + 1)
	// After - let i := 3 ~ i + 1
	private Funp inlineLambdas(Funp node) {
		var defByVariables = Funp_.associateDefinitions(node);

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					if (n_ instanceof FunpApply apply //
							&& lookup(defByVariables, apply.lambda) instanceof FunpLambda lambda //
							&& lambda.fct != Fct.ONCE__)
						return FunpDefine.of(lambda.vn, inline(apply.value), inline(lambda.expr), Fdt.L_MONO);
					else
						return null;
				});
			}
		}.inline(node);
	}

	// Before - define s := t:3 ~ if (`t:v` = s) then v else 0
	// After - define s := t:3 ~ 3
	private Funp inlineTags(Funp node) {
		var defByVariables = Funp_.associateDefinitions(node);

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					if (n_ instanceof FunpTagId tagId //
							&& lookup(defByVariables, tagId.reference.expr) instanceof FunpTag tag)
						return FunpNumber.of(tag.id);
					else if (n_ instanceof FunpTagValue tagValue //
							&& lookup(defByVariables, tagValue.reference.expr) instanceof FunpTag tag)
						return Equals.string(tag.tag, tagValue.tag) ? tag.value : FunpDontCare.of();
					else
						return null;
				});
			}
		}.inline(node);
	}

	private Funp lookup(Map<FunpVariable, Funp> defByVariables, Funp expr) {
		if (expr instanceof FunpVariable variable //
				&& defByVariables.get(variable) instanceof FunpDefine define //
				&& Fdt.isLocal(define.fdt) //
				&& Fdt.isPure(define.fdt))
			return lookup(defByVariables, define.value);
		else
			return expr;
	}

}
