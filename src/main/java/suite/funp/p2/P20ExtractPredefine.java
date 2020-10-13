package suite.funp.p2;

import java.util.ArrayList;

import primal.MoreVerbs.Read;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.Fpt;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpLambdaFree;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpVariable;
import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class P20ExtractPredefine {

	private Inspect inspect = Singleton.me.inspect;

	public Funp extractPredefine(Funp node0) {
		var vns = new ArrayList<String>();

		var node1 = new Object() {
			private Funp extract(Funp n) {
				return inspect.rewrite(n, Funp.class, n_ -> {
					return n_.sw( //
					).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
						return FunpLambda.of(vn, extractPredefine(expr), fct);
					})).applyIf(FunpPredefine.class, f -> f.apply((vn, expr, fpt) -> {
						var var = FunpVariable.of(vn);
						if (vns.add(vn)) {
							var assignVar = FunpDoAssignVar.of(var, extract(expr), var);
							return fpt == Fpt.FREE_ ? FunpLambdaFree.of(var, assignVar) : assignVar;
						} else
							return var;
					})).result();
				});
			}
		}.extract(node0);

		return Read.from(vns).fold(node1, (n, vn) -> FunpDefine.of(vn, FunpDontCare.of(), n, Fdt.L_MONO));
	}

}
