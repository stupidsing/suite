package suite.funp;

import java.util.HashMap;
import java.util.Map;

import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.primitive.IntMutable;
import suite.util.Switch;

public class P1Inline {

	private Inspect inspect = Singleton.me.inspect;

	public Funp inline(Funp node) {
		return Boolean.TRUE ? node : inline_(node);
	}

	private Funp inline_(Funp node) {
		Map<FunpDefine, IntMutable> counts = new HashMap<>();

		new Object() {
			private Funp count(IMap<String, FunpDefine> vars, Funp node0) {
				return inspect.rewrite(Funp.class, n_ -> {
					if (n_ instanceof FunpDefine) {
						FunpDefine n1 = (FunpDefine) n_;
						counts.put(n1, IntMutable.of(0));
						count(vars, n1.value);
						count(vars.put(n1.var, n1), n1.expr);
					} else if (n_ instanceof FunpVariable) {
						FunpDefine def = vars.get(((FunpVariable) n_).var);
						if (def != null)
							counts.get(def).increment();
					}
					return null;
				}, node0);
			}
		}.count(IMap.empty(), node);

		return new Object() {
			private Funp expand(IMap<String, Funp> vars, Funp node0) {
				return inspect.rewrite(Funp.class, n_ -> {
					return new Switch<Funp>(n_ //
					).applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
						IMap<String, Funp> vars1 = vars.put(var, (FunpDefine) n_);
						return 1 < counts.get(n_).get() //
								? FunpDefine.of(var, expand(vars, value), expand(vars1, expr)) //
								: expand(vars1, expr);
					})).applyIf(FunpVariable.class, f -> f.apply(var -> {
						Funp def0 = vars.get(var);
						FunpDefine def1;
						if (def0 instanceof FunpDefine && counts.get(def1 = (FunpDefine) def0).get() == 1)
							return def1.value;
						else
							return n_;
					})).result();
				}, node0);
			}
		}.expand(IMap.empty(), node);
	}

}
