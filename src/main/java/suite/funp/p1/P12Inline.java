package suite.funp.p1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.Verbs.Reverse;
import primal.adt.Pair;
import primal.fp.Funs.Iterate;
import primal.persistent.PerMap;
import primal.primitive.adt.IntMutable;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpLambda;
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
			node = inlineFields(node);
			node = inlineLambdas(node);
			node = inlineTags(node);
		}

		for (var i = 0; i < rounds; i++)
			node = inlineDefines(node);

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
				).applyIf(FunpDefine.class, f -> f.apply((vn0, value, expr, fdt) -> {
					var vn1 = newVarName.apply(vn0);
					var r1 = new Rename(vns.replace(vn0, vn1));
					return FunpDefine.of(vn1, rename(value), r1.rename(expr), fdt);
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

	// Before - define i := memory ~ assign i := value ~ expr
	// After - define i := value ~ expr
	private Funp inlineDefineAssigns(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n0 -> {
					var vns = new ArrayList<Pair<String, Fdt>>();
					FunpDoAssignVar assign;
					FunpTypeCheck check;
					FunpDefine define;

					while ((define = n0.cast(FunpDefine.class)) != null //
							&& Fdt.isLocal(define.fdt) //
							&& define.value instanceof FunpDontCare) {
						vns.add(Pair.of(define.vn, define.fdt));
						n0 = define.expr;
					}

					if ((check = n0.cast(FunpTypeCheck.class)) != null)
						n0 = check.expr;

					if ((assign = n0.cast(FunpDoAssignVar.class)) != null) {
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

	// Before - define i := 1 ~ i + 1
	// After - 1 + 1
	private Funp inlineDefines(Funp node) {
		var defByVariables = Funp_.associateDefinitions(node);
		var countByDefs = new HashMap<Funp, IntMutable>();

		new Object() {
			public void count(Funp node_, boolean isWithinIo) {
				inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
					if (isWithinIo) // too dangerous to inline imperative code
						getCount(f).update(9999);
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
				var def = defByVariables.get(var);
				return getCount(def);
			}

			private IntMutable getCount(Funp def) {
				return countByDefs.computeIfAbsent(def, v -> IntMutable.of(0));
			}
		}.count(node, false);

		var zero = IntMutable.of(0);

		var defines = Read //
				.from2(defByVariables) //
				.values() //
				.distinct() //
				.filter(def -> def instanceof FunpDefine && countByDefs.getOrDefault(def, zero).value() <= 1) //
				.map2(def -> (FunpDefine) def) //
				.filterValue(def -> Fdt.isLocal(def.fdt) && Fdt.isPure(def.fdt)) //
				.toMap();

		var expands = Read //
				.from2(defByVariables) //
				.mapValue(defines::get) //
				.filterValue(def -> def != null) //
				.toMap();

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					FunpDefine define;
					if ((define = defines.get(n_)) != null)
						return inline(define.expr);
					else if ((define = expands.get(n_)) != null)
						return inline(define.value);
					else
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
					FunpDefine define;
					FunpField field;
					FunpStruct struct;
					FunpVariable variable;
					if ((field = n_.cast(FunpField.class)) != null //
							&& (variable = field.reference.expr.cast(FunpVariable.class)) != null //
							&& (define = defByVariables.get(variable).cast(FunpDefine.class)) != null //
							&& (define.fdt == Fdt.L_MONO || define.fdt == Fdt.L_POLY) //
							&& (struct = define.value.cast(FunpStruct.class)) != null) {
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
	// After - 3 + 1
	private Funp inlineLambdas(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> n_.sw() //
						.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
							return lambda.castMap(FunpLambda.class,
									l -> FunpDefine.of(l.vn, inline(value), inline(l.expr), Fdt.L_MONO));
						})) //
						.result());
			}
		}.inline(node);
	}

	// Before - define s := t:3 ~ if (`t:v` = s) then v else 0
	// After - define s := t:3 ~ 3
	private Funp inlineTags(Funp node) {
		var defs = Funp_.associateDefinitions(node);

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					FunpTag tag;
					FunpTagId tagId;
					FunpTagValue tagValue;
					FunpVariable variable;
					if ((tagId = n_.cast(FunpTagId.class)) != null //
							&& (variable = tagId.reference.expr.cast(FunpVariable.class)) != null //
							&& (tag = defs.get(variable).castMap(FunpDefine.class,
									n -> n.value.cast(FunpTag.class))) != null)
						return FunpNumber.of(tag.id);
					else if ((tagValue = n_.cast(FunpTagValue.class)) != null //
							&& (variable = tagValue.reference.expr.cast(FunpVariable.class)) != null //
							&& (tag = defs.get(variable).castMap(FunpDefine.class,
									n -> n.value.cast(FunpTag.class))) != null)
						return Equals.string(tag.tag, tagValue.tag) ? tag.value : FunpDontCare.of();
					else
						return null;
				});
			}
		}.inline(node);
	}

}
