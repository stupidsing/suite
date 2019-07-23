package suite.funp.p3;

import suite.adt.pair.Pair;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoWhile;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOp;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.primitive.IntInt_Bool;
import suite.primitive.IntInt_Int;
import suite.primitive.IntRange;
import suite.streamlet.FunUtil2.Fun2;
import suite.streamlet.Read;

public class P3Optimize {

	private Inspect inspect = Singleton.me.inspect;

	private int ps = Funp_.pointerSize;

	public Funp optimize(Funp n) {
		return inspect.rewrite(n, Funp.class, this::optimize_);
	}

	private Funp optimize_(Funp n) {
		return n.sw( //
		).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
			return expr instanceof FunpDontCare ? optimize(expr) : n;
		})).applyIf(FunpData.class, f -> f.apply(pairs -> {
			return FunpData.of(Read.from2(pairs).concatMap((expr, range) -> {
				var expr1 = optimize(expr);
				var start = range.s;
				var pairsx = expr1.cast(FunpData.class, g -> g.apply(pairs1 -> Read //
						.from2(pairs1) //
						.map((exprc, range1) -> Pair.of(optimize(exprc), IntRange.of(start + range1.s, start + range1.e)))));
				return pairsx != null ? pairsx : Read.each(Pair.of(expr1, range));
			}).toList());
		})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
			return optimize(pointer).sw().applyIf(FunpReference.class, g -> g.expr).result();
		})).applyIf(FunpDoWhile.class, f -> f.apply((while_, do_, expr) -> {
			return optimize(while_).sw().applyIf(FunpBoolean.class, g -> g.apply(b -> b ? null : expr)).result();
		})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
			return optimize(if_).sw().applyIf(FunpBoolean.class, g -> g.apply(b -> b ? then : else_)).result();
		})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
			return optimize(pointer).sw( //
			).applyIf(FunpData.class, g -> g.apply(pairs -> {
				for (var pair : pairs) {
					var range = pair.v;
					if (start == range.s && end == range.e)
						return pair.k;
				}
				return null;
			})).applyIf(FunpReference.class, g -> {
				return FunpOp.of(ps, TermOp.PLUS__, g.expr, FunpNumber.ofNumber(start));
			}).result();
		})).applyIf(FunpReference.class, f -> f.apply(expr -> {
			return optimize(expr).sw().applyIf(FunpMemory.class, g -> g.pointer).result();
		})).applyIf(FunpOp.class, f -> f.apply((opSize, op, lhs, rhs) -> {
			var iib = TreeUtil.boolOperations.get(op);
			var iii = TreeUtil.intOperations.get(op);
			var iit = TreeUtil.tupleOperations.get(op);
			Funp f1 = null;
			f1 = iib != null ? evaluate(iib, lhs, rhs) : null;
			f1 = iii != null ? evaluate(iii, lhs, rhs) : null;
			f1 = iit != null ? evaluate(iit, lhs, rhs) : null;
			return f1;
		})).result();
	}

	private FunpNumber evaluate(IntInt_Int fun, Funp lhs0, Funp rhs0) {
		return fun != null ? evaluate(lhs0, rhs0, (lhs1, rhs1) -> FunpNumber.ofNumber(fun.apply(lhs1, rhs1))) : null;
	}

	private FunpBoolean evaluate(IntInt_Bool fun, Funp lhs0, Funp rhs0) {
		return fun != null ? evaluate(lhs0, rhs0, (lhs1, rhs1) -> FunpBoolean.of(fun.apply(lhs1, rhs1))) : null;
	}

	private <T> T evaluate(Funp lhs0, Funp rhs0, Fun2<Integer, Integer, T> fun) {
		var lhs1 = optimize(lhs0).cast(FunpNumber.class, n -> n.i.value());
		var rhs1 = optimize(rhs0).cast(FunpNumber.class, n -> n.i.value());
		return lhs1 != null && rhs1 != null ? fun.apply(lhs1, rhs1) : null;
	}

}
