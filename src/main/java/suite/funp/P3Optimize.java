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
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpWhile;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.node.util.TreeUtil.IntInt_Bool;
import suite.primitive.IntInt_Int;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Switch;

public class P3Optimize {

	private Inspect inspect = Singleton.me.inspect;

	public Funp optimize(Funp n) {
		return inspect.rewrite(Funp.class, this::optimize_, n);
	}

	private Funp optimize_(Funp n) {
		return n.<Funp> switch_( //
		).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
			return !(expr instanceof FunpDontCare) ? n : optimize(expr);
		})).applyIf(FunpData.class, f -> f.apply(pairs -> {
			return FunpData.of(Read.from2(pairs).concatMap((expr, range) -> {
				Funp expr1 = optimize(expr);
				int start = range.t0;
				Streamlet<Pair<Funp, IntIntPair>> pairsx = new Switch<Streamlet<Pair<Funp, IntIntPair>>>(expr1 //
				).applyIf(FunpData.class, g -> g.apply(pairs1 -> {
					return Read //
							.from2(pairs1) //
							.map((exprc, range1) -> Pair.of(optimize(exprc), IntIntPair.of(start + range1.t0, start + range1.t1)));
				})).result();
				return pairsx != null ? pairsx : Read.each(Pair.of(expr1, range));
			}).toList());
		})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
			return optimize(pointer).<Funp> switch_().applyIf(FunpReference.class, g -> g.expr).result();
		})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
			return optimize(if_) //
					.<Funp> switch_() //
					.applyIf(FunpBoolean.class, g -> g.apply(b -> {
						return b ? then : else_;
					})).result();
		})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
			return optimize(pointer) //
					.<Funp> switch_() //
					.applyIf(FunpData.class, g -> g.apply(pairs -> {
						for (Pair<Funp, IntIntPair> pair : pairs) {
							IntIntPair range = pair.t1;
							if (start == range.t0 && end == range.t1)
								return pair.t0;
						}
						return null;
					})).applyIf(FunpReference.class, g -> {
						return FunpTree.of(TermOp.PLUS__, g.expr, FunpNumber.ofNumber(start));
					}).result();
		})).applyIf(FunpReference.class, f -> f.apply(expr -> {
			return optimize(expr).<Funp> switch_().applyIf(FunpMemory.class, g -> g.pointer).result();
		})).applyIf(FunpTree.class, f -> f.apply((operator, lhs, rhs) -> {
			IntInt_Bool iib = TreeUtil.boolOperations.get(operator);
			IntInt_Int iii = TreeUtil.intOperations.get(operator);
			if (iib != null)
				return evaluate(iib, lhs, rhs);
			else if (iii != null)
				return evaluate(iii, lhs, rhs);
			else
				return null;
		})).applyIf(FunpTree2.class, f -> f.apply((operator, lhs, rhs) -> {
			return evaluate(TreeUtil.tupleOperations.get(operator), lhs, rhs);
		})).applyIf(FunpWhile.class, f -> f.apply((while_, do_, expr) -> {
			return optimize(while_) //
					.<Funp> switch_() //
					.applyIf(FunpBoolean.class, g -> g.apply(b -> {
						return b ? null : expr;
					})).result();
		})).result();
	}

	private FunpNumber evaluate(IntInt_Int fun, Funp lhs0, Funp rhs0) {
		Integer[] pair = evaluate(lhs0, rhs0);
		Integer lhs1 = pair[0];
		Integer rhs1 = pair[1];
		return fun != null && lhs1 != null && rhs1 != null ? FunpNumber.ofNumber(fun.apply(lhs1, rhs1)) : null;
	}

	private FunpBoolean evaluate(IntInt_Bool fun, Funp lhs0, Funp rhs0) {
		Integer[] pair = evaluate(lhs0, rhs0);
		Integer lhs1 = pair[0];
		Integer rhs1 = pair[1];
		return fun != null && lhs1 != null && rhs1 != null ? FunpBoolean.of(fun.apply(lhs1, rhs1)) : null;
	}

	private Integer[] evaluate(Funp lhs0, Funp rhs0) {
		Integer lhs2 = optimize(lhs0).cast(FunpNumber.class, n -> n.i.get());
		Integer rhs2 = optimize(rhs0).cast(FunpNumber.class, n -> n.i.get());
		return new Integer[] { lhs2, rhs2, };
	}

}
