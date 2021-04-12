package suite.funp.p2;

import static primal.statics.Fail.fail;

import java.util.ArrayList;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.Fpt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpLambdaFree;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpVariable;
import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class P20ExtractPredefine {

	private Inspect inspect = Singleton.me.inspect;

	public Funp extractPredefine(Funp node0) {
		var defers = new ArrayList<Iterate<Funp>>();
		var vns = new ArrayList<String>();

		var node1 = new Object() {
			private Funp extract(Funp n) {
				return inspect.rewrite(n, Funp.class, n_ -> {
					return n_.sw( //
					).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
						return FunpDefine.of(vn, extract(value), extractPredefine(expr), fdt);
					})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
						return FunpDefineRec.of( //
								Read.from2(pairs).mapValue(this::extract).toList(), //
								extractPredefine(expr), //
								fdt);
					})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
						return FunpLambda.of(vn, extractPredefine(expr), fct);
					})).applyIf(FunpPredefine.class, f -> f.apply((vn, expr, fpt, df, dv) -> {
						var vn_ = vn != null ? vn : "predefine$" + Get.temp();
						var var = FunpVariable.of(vn_);
						if (vns.add(vn_)) {
							var expr_ = extract(expr);
							if (fpt == Fpt.APPLY_)
								defers.add(f_ -> {
									var vnResult = "result$" + Get.temp();
									var varResult = FunpVariable.of(vnResult);
									var apply = FunpApply.of(var, dv);
									var fda = FunpDefine.of("unused$" + Get.temp(), apply, varResult, Fdt.L_IOAP);
									return FunpDefine.of(vnResult, f_, fda, Fdt.L_MONO);
								});
							else if (fpt == Fpt.FREE__)
								defers.add(f_ -> FunpLambdaFree.of(FunpReference.of(var), f_));
							else if (fpt == Fpt.INVOKE)
								defers.add(f_ -> {
									var vnResult = "result$" + Get.temp();
									var varResult = FunpVariable.of(vnResult);
									var field = FunpField.of(FunpReference.of(var), df);
									var apply = FunpApply.of(FunpDontCare.of(), field);
									var fda = FunpDefine.of("unused$" + Get.temp(), apply, varResult, Fdt.L_IOAP);
									return FunpDefine.of(vnResult, f_, fda, Fdt.L_MONO);
								});
							else if (fpt == Fpt.NONE__)
								;
							else
								return fail();
							return FunpDoAssignVar.of(var, expr_, var);
						} else
							return var;
					})).result();
				});
			}
		}.extract(node0);

		var node2 = Read.from(defers).fold(node1, (n, defer) -> defer.apply(n));
		var node3 = Read.from(vns).fold(node2, (n, vn) -> FunpDefine.of(vn, FunpDontCare.of(), n, Fdt.L_MONO));

		return node3;
	}

}
