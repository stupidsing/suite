package suite.funp;

import java.util.List;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpTco;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.util.String_;
import suite.util.Switch;

public class P1ReduceTailCall {

	private Inspect inspect = Singleton.me.inspect;

	public Funp reduce(Funp node) {
		return inspect.rewrite(Funp.class, node_ -> {
			return new Switch<Funp>(node_ //
			).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				if (pairs.size() == 1) {
					var pair = pairs.get(0);
					var lambdaVar = pair.t0;
					var lambda1 = new Switch<Funp>(pair.t1 //
					).applyIf(FunpLambda.class, g -> g.apply((var, do_) -> {
						return !isHasLambda(do_) ? FunpLambda.of(var, rewriteTco(lambdaVar, do_)) : null;
					})).result();
					return lambda1 != null ? FunpDefineRec.of(List.of(Pair.of(lambdaVar, lambda1)), expr) : null;
				} else
					return null;
			})).result();
		}, node);
	}

	private Funp rewriteTco(String var, Funp do_) {
		var o = new Object() {
			private boolean b = false;

			private Funp tco(Funp do_) {
				return new Switch<Funp>(do_ //
				).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
					return new Switch<Funp>(lambda //
					).applyIf(FunpVariable.class, g -> g.apply(var_ -> {
						return String_.equals(var, var_) ? tco(true, value, FunpDontCare.of()) : null;
					})).result();
				})).applyIf(FunpIf.class, g -> g.apply((if_, then, else_) -> {
					return FunpIf.of(if_, tco(then), tco(else_));
				})).applyIf(Funp.class, g -> {
					return tco(false, FunpDontCare.of(), g);
				}).result();
			}

			private Funp tco(boolean c, Funp n, Funp r) {
				b |= c;
				return FunpTco.of(var, FunpStruct.of(List.of( //
						Pair.of("c", FunpBoolean.of(c)), //
						Pair.of("n", n), //
						Pair.of("r", r))));
			}
		};
		var do1 = o.tco(do_);
		return o.b ? do1 : do_;
	}

	private boolean isHasLambda(Funp node) {
		var b = new Object() {
			private boolean b = false;

			private Funp r(Funp node_) {
				b |= node_ instanceof FunpLambda;
				return null;
			}
		};

		inspect.rewrite(Funp.class, b::r, node);
		return b.b;
	}

}
