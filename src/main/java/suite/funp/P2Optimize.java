package suite.funp;

import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.primitive.IntInt_Int;
import suite.util.Switch;

public class P2Optimize {

	private Inspect inspect = Singleton.me.inspect;

	public Funp optimize(Funp n) {
		return inspect.rewrite(Funp.class, this::optimize_, n);
	}

	private Funp optimize_(Funp n) {
		return new Switch<Funp>(n //
		).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
			return new Switch<Funp>(pointer //
			).applyIf(FunpReference.class, g -> g.expr).result();
		})).applyIf(FunpTree.class, f -> f.apply((operator, lhs0, rhs0) -> {
			return evaluate(TreeUtil.intOperations.get(operator), lhs0, rhs0);
		})).applyIf(FunpTree2.class, f -> f.apply((operator, lhs, rhs) -> {
			return evaluate(TreeUtil.tupleOperations.get(operator), lhs, rhs);
		})).result();
	}

	private FunpNumber evaluate(IntInt_Int fun, Funp lhs0, Funp rhs0) {
		Funp lhs1 = optimize(lhs0);
		Funp rhs1 = optimize(rhs0);
		Integer lhs2 = lhs1 instanceof FunpNumber ? ((FunpNumber) lhs1).i : null;
		Integer rhs2 = rhs1 instanceof FunpNumber ? ((FunpNumber) rhs1).i : null;
		return fun != null && lhs2 != null && rhs2 != null ? FunpNumber.of(fun.apply(lhs2, rhs2)) : null;
	}

}
