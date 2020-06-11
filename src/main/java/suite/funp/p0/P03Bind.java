package suite.funp.p0;

import static suite.util.Streamlet_.forInt;

import primal.persistent.PerSet;
import primal.primitive.IntPrim.IntObj_Obj;
import primal.primitive.IntPrim.Int_Obj;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTag;
import suite.funp.P0.FunpTagId;
import suite.funp.P0.FunpTagValue;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.node.io.TermOp;
import suite.util.Switch;

public class P03Bind {

	private PerSet<String> vns;

	public P03Bind(PerSet<String> vns) {
		this.vns = vns;
	}

	public Funp bind(Funp be, Funp value, Funp then, Funp else_) {
		IntObj_Obj<Int_Obj<Funp>, Funp> bindArray = (size0, fun0) -> {
			var fun1 = new Switch<Int_Obj<Funp>>(value //
			).applyIf(FunpArray.class, g -> {
				var elements = g.elements;
				return size0 == elements.size() ? elements::get : null;
			}).applyIf(FunpRepeat.class, g -> g.apply((count, expr) -> {
				Int_Obj<Funp> fun_ = i -> expr;
				return size0 == count ? fun_ : null;
			})).applyIf(Funp.class, g -> {
				return i -> FunpIndex.of(FunpReference.of(value), FunpNumber.ofNumber(i));
			}).result();

			return forInt(size0).fold(then, (i, then_) -> bind(fun0.apply(i), fun1.apply(i), then_, else_));
		};

		if (be instanceof FunpBoolean && value instanceof FunpBoolean)
			return ((FunpBoolean) be).b == ((FunpBoolean) value).b ? then : else_;
		else if (be instanceof FunpNumber && value instanceof FunpNumber)
			return ((FunpNumber) be).i == ((FunpNumber) value).i ? then : else_;
		else {
			var result = be.sw( //
			).applyIf(FunpArray.class, f -> f.apply(elements0 -> {
				return bindArray.apply(elements0.size(), elements0::get);
			})).applyIf(FunpDontCare.class, f -> {
				return then;
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return bind(expr, FunpDeref.of(value), then, else_);
			})).applyIf(FunpRepeat.class, f -> f.apply((size0, expr0) -> {
				return bindArray.apply(size0, i -> expr0);
			})).applyIf(FunpStruct.class, f -> f.apply((isCompleted, pairs0) -> {
				var pairs1 = value.cast(FunpStruct.class, g -> g.pairs);
				var size0 = pairs0.size();

				Int_Obj<Funp> fun = pairs1 != null && size0 == pairs1.size() //
						? i -> pairs1.get(i).v //
						: i -> FunpField.of(FunpReference.of(value), pairs0.get(i).k);

				return forInt(size0).fold(then, (i, then_) -> bind(pairs0.get(i).v, fun.apply(i), then_, else_));
			})).applyIf(FunpTag.class, f -> f.apply((id, tag, value_) -> {
				return new Switch<Funp>(value //
				).applyIf(FunpTag.class, g -> g.apply((id1, tag1, value1) -> {
					return id.value() == id1.value() ? bind(value_, value1, then, else_) : else_;
				})).applyIf(Funp.class, g -> {
					var ref = FunpReference.of(value);

					// FIXME double else
					var bind = bind(value_, FunpTagValue.of(ref, tag), then, else_);
					return FunpIf.of(FunpTree.of(TermOp.EQUAL_, FunpNumber.of(id), FunpTagId.of(ref)), bind, else_);
				}).result();
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return vns.contains(var) ? FunpDoAssignVar.of(f, value, then) : be;
			})).result();

			return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
		}
	}


}
