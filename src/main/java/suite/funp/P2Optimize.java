package suite.funp;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P1.FunpData;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpWhile;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.node.util.TreeUtil.IntInt_Bool;
import suite.primitive.IntInt_Int;
import suite.primitive.adt.pair.IntIntPair;
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
		})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
			return new Switch<Funp>(optimize(pointer)).applyIf(FunpReference.class, g -> g.expr).result();
		})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
			return new Switch<Funp>(optimize(if_) //
			).applyIf(FunpBoolean.class, g -> g.apply(b -> {
				return b ? then : else_;
			})).result();
		})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
			return new Switch<Funp>(optimize(pointer) //
			).applyIf(FunpData.class, g -> g.apply(pairs -> {
				for (Pair<Funp, IntIntPair> pair : pairs) {
					IntIntPair range = pair.t1;
					return start == range.t0 && end == range.t1 ? pair.t0 : null;
				}
				return null;
			})).applyIf(FunpReference.class, g -> {
				return FunpTree.of(TermOp.PLUS__, g.expr, FunpNumber.ofNumber(start));
			}).result();
		})).applyIf(FunpReference.class, f -> f.apply(expr -> {
			return new Switch<Funp>(optimize(expr)).applyIf(FunpMemory.class, g -> g.pointer).result();
		})).applyIf(FunpTree.class, f -> f.apply((operator, lhs, rhs) -> {
			IntInt_Bool iib = TreeUtil.boolOperations.get(operator);
			IntInt_Int iii = TreeUtil.intOperations.get(operator);
			if (iib != null)
				return evaluate(iib, lhs, rhs);
			if (iii != null)
				return evaluate(iii, lhs, rhs);
			else
				return null;
		})).applyIf(FunpTree2.class, f -> f.apply((operator, lhs, rhs) -> {
			return evaluate(TreeUtil.tupleOperations.get(operator), lhs, rhs);
		})).applyIf(FunpWhile.class, f -> f.apply((while_, do_, expr) -> {
			return new Switch<Funp>(optimize(while_) //
			).applyIf(FunpBoolean.class, g -> g.apply(b -> {
				return b ? null : expr;
			})).result();
		})).result();
	}

	private FunpNumber evaluate(IntInt_Int fun, Funp lhs0, Funp rhs0) {
		Integer[] pair = evaluate(lhs0, rhs0);
		Integer lhs3 = pair[0];
		Integer rhs3 = pair[1];
		return fun != null && lhs3 != null && rhs3 != null ? FunpNumber.ofNumber(fun.apply(lhs3, rhs3)) : null;
	}

	private FunpBoolean evaluate(IntInt_Bool fun, Funp lhs0, Funp rhs0) {
		Integer[] pair = evaluate(lhs0, rhs0);
		Integer lhs3 = pair[0];
		Integer rhs3 = pair[1];
		return fun != null && lhs3 != null && rhs3 != null ? FunpBoolean.of(fun.apply(lhs3, rhs3)) : null;
	}

	private Integer[] evaluate(Funp lhs0, Funp rhs0) {
		Funp lhs1 = optimize(lhs0);
		Funp rhs1 = optimize(rhs0);
		Integer lhs2 = lhs1 instanceof FunpNumber ? ((FunpNumber) lhs1).i.get() : null;
		Integer rhs2 = rhs1 instanceof FunpNumber ? ((FunpNumber) rhs1).i.get() : null;
		return new Integer[] { lhs2, rhs2, };
	}

}
