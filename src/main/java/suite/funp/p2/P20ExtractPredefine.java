package suite.funp.p2;

import static primal.statics.Fail.fail;

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

import java.util.ArrayList;

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
						var var = FunpVariable.of(vn);
						if (vns.add(vn)) {
							var expr_ = extract(expr);
							if (fpt == Fpt.APPLY_) {
								var apply = FunpApply.of(var, dv);
								defers.add(f_ -> FunpDefine.of("deferred-apply$" + Get.temp(), apply, f_, Fdt.L_IOAP));
							} else if (fpt == Fpt.FREE__)
								defers.add(f_ -> FunpLambdaFree.of(var, f_));
							else if (fpt == Fpt.INVOKE) {
								var apply = FunpApply.of(FunpDontCare.of(), FunpField.of(FunpReference.of(var), df));
								defers.add(f_ -> FunpDefine.of("deferred-invoke" + Get.temp(), apply, f_, Fdt.L_IOAP));
							} else if (fpt == Fpt.NONE__)
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
