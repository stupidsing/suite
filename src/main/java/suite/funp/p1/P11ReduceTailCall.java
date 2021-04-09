package suite.funp.p1;

import java.util.List;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Pair;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDoWhile;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpVariable;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.util.Switch;

public class P11ReduceTailCall {

	private Inspect inspect = Singleton.me.inspect;

	public P11ReduceTailCall(Funp_ f) {
	}

	public Funp reduce(Funp node) {
		return inspect.rewrite(node, Funp.class, node_ -> {
			List<Pair<String, Funp>> pairs;
			Pair<String, Funp> pair;
			String lambdaVar;
			Funp lambda1;
			if (node_ instanceof FunpDefineRec defs //
					&& (pairs = defs.pairs).size() == 1 //
					&& (pair = pairs.get(0)).v instanceof FunpLambda g //
					&& !isHasLambda(g.expr) //
					&& (lambda1 = rewriteTco(lambdaVar = pair.k, g.vn, g.expr)) != null) {
				return FunpDefineRec.of(List.of(Pair.of(lambdaVar, lambda1)), defs.expr, defs.fdt);
			} else
				return null;
		});
	}

	private Funp rewriteTco(String lambdaVn, String vn, Funp do_) {
		var o = new Object() {
			private boolean b = false;

			private Funp tco(Funp do_) {
				return new Switch<Funp>(do_ //
				).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
					return lambda instanceof FunpVariable fv && Equals.string(lambdaVn, fv.vn) //
							? cont(true, value, FunpDontCare.of()) //
							: null;
				})).applyIf(FunpIf.class, g -> g.apply((if_, then, else_) -> {
					return FunpIf.of(if_, tco(then), tco(else_));
				})).applyIf(Funp.class, g -> {
					return cont(false, FunpDontCare.of(), g);
				}).result();
			}

			private Funp cont(boolean c, Funp n, Funp r) {
				b |= c;
				return FunpStruct.of(List.of( //
						Pair.of("c", FunpBoolean.of(c)), //
						Pair.of("n", n), //
						Pair.of("r", r)));
			}
		};

		var do1 = o.tco(do_);
		// return o.b ? FunpTco.of(var, do1) : null;

		if (o.b) {
			var tcoVarName = "tco$" + Get.temp();
			var var = FunpVariable.of(vn);
			var tcoVar = FunpVariable.of(tcoVarName);
			var tcoVarRef = FunpReference.of(tcoVar);
			var fc = FunpField.of(tcoVarRef, "c");
			var fn = FunpField.of(tcoVarRef, "n");
			var fr = FunpField.of(tcoVarRef, "r");
			var dontCare = FunpDontCare.of();
			var assign = FunpDoAssignVar.of(tcoVar, do1, FunpDoAssignVar.of(var, fn, fc));
			var while_ = FunpDoWhile.of(assign, FunpDontCare.of(), fr);
			return FunpLambda.of(vn, FunpDefine.of(tcoVarName, dontCare, while_, Fdt.L_MONO));
		} else
			return null;
	}

	private boolean isHasLambda(Funp node) {
		var b = new Object() {
			private boolean b = false;

			private Funp r(Funp node_) {
				b |= node_ instanceof FunpLambda;
				return null;
			}
		};

		inspect.rewrite(node, Funp.class, b::r);
		return b.b;
	}

}
