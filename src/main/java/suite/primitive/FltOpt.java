package suite.primitive;

import java.util.Objects;

import suite.adt.Opt;
import suite.primitive.FltPrimitives.FltPredicate;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.util.Object_;

public class FltOpt {

	private static FltOpt none_ = FltOpt.of(FltFunUtil.EMPTYVALUE);
	private float value;

	public static FltOpt none() {
		return none_;
	}

	public static FltOpt of(float t) {
		FltOpt p = new FltOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == FltFunUtil.EMPTYVALUE;
	}

	public FltOpt filter(FltPredicate pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Flt_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public float get() {
		if (!isEmpty())
			return value;
		else
			throw new RuntimeException("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == FltOpt.class && Objects.equals(value, ((FltOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != FltFunUtil.EMPTYVALUE ? Float.toString(value) : "null";
	}

}
