package suite.funp;

import java.util.List;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpStruct;
import suite.funp.P1.FunpTco;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.primitive.IntMutable;
import suite.util.Switch;

public class P1ReduceTailCall {

	private Inspect inspect = Singleton.me.inspect;

	public Funp reduce(Funp node) {
		return inspect.rewrite(Funp.class, this::reduce_, node);
	}

	private Funp reduce_(Funp node) {
		return new Switch<Funp>(node //
		).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
			if (pairs.size() == 1) {
				var pair = pairs.get(0);
				var lambdaVar = pair.t0;
				var lambda1 = new Switch<Funp>(pair.t1 //
				).applyIf(FunpLambda.class, g -> g.apply((var, do_) -> {
					var rt = new RewriteTco();
					rt.var = lambdaVar;
					return !isHasLambda(do_) ? FunpLambda.of(var, rt.rewrite(do_)) : null;
				})).result();
				return lambda1 != null ? FunpDefineRec.of(List.of(Pair.of(lambdaVar, lambda1)), expr) : null;
			} else
				return null;
		})).result();
	}

	private class RewriteTco {
		private String var;

		private Funp rewrite(Funp do_) {
			return FunpTco.of(var, FunpStruct.of(List.of( //
					Pair.of("c", FunpBoolean.of(false)), //
					Pair.of("n", FunpDontCare.of()), //
					Pair.of("r", do_))));
		}
	}

	private boolean isHasLambda(Funp node) {
		IntMutable b = IntMutable.of(0);
		inspect.rewrite(Funp.class, node_ -> {
			if (node_ instanceof FunpLambda)
				b.update(1);
			return null;
		}, node);
		return b.get() != 0;
	}

}
