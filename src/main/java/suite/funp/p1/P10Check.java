package suite.funp.p1;

import primal.MoreVerbs.Read;
import primal.persistent.PerSet;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpLambda;
import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class P10Check {

	private Inspect inspect = Singleton.me.inspect;

	public P10Check(Funp_ f) {
	}

	public Funp check(Funp node) {
		checkDuplicateDefinitions(PerSet.empty(), node);
		return node;
	}

	private Funp checkDuplicateDefinitions(PerSet<String> vns, Funp node) {
		return new Object() {
			private Funp c(Funp n) {
				return inspect.rewrite(n, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
					checkDuplicateDefinitions(vns, value);
					return checkDuplicateDefinitions(vns.add(vn), expr);
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
					var vns1 = Read.from(pairs).fold(vns, (vns_, pair) -> vns.add(pair.k));
					for (var pair : pairs)
						checkDuplicateDefinitions(vns1, pair.v);
					return checkDuplicateDefinitions(vns1, expr);
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
					return checkDuplicateDefinitions(PerSet.<String> empty().add(vn), expr);
				})).result());
			}
		}.c(node);
	}

}
