package suite.funp;

import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P1.FunpMemory;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
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
		).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
			return !(expr instanceof FunpDontCare) ? n : optimize(expr);
		})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
			return new Switch<Funp>(optimize(pointer) //
			).applyIf(FunpReference.class, g -> {
				return FunpTree.of(TermOp.PLUS__, g.expr, FunpNumber.ofNumber(start));
			}).result();
		})).applyIf(FunpReference.class, f -> f.apply(expr -> {
			return new Switch<Funp>(optimize(expr) //
			).applyIf(FunpMemory.class, g -> g.pointer).result();
		})).applyIf(FunpTree.class, f -> f.apply((operator, lhs, rhs) -> {
			return evaluate(TreeUtil.intOperations.get(operator), lhs, rhs);
		})).applyIf(FunpTree2.class, f -> f.apply((operator, lhs, rhs) -> {
			return evaluate(TreeUtil.tupleOperations.get(operator), lhs, rhs);
		})).result();
	}

	private FunpNumber evaluate(IntInt_Int fun, Funp lhs0, Funp rhs0) {
		Funp lhs1 = optimize(lhs0);
		Funp rhs1 = optimize(rhs0);
		Integer lhs2 = lhs1 instanceof FunpNumber ? ((FunpNumber) lhs1).i.get() : null;
		Integer rhs2 = rhs1 instanceof FunpNumber ? ((FunpNumber) rhs1).i.get() : null;
		return fun != null && lhs2 != null && rhs2 != null ? FunpNumber.ofNumber(fun.apply(lhs2, rhs2)) : null;
	}

}
