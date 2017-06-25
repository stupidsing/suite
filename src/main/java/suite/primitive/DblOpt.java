package suite.primitive;

import java.util.Objects;

import suite.adt.Opt;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.util.Object_;

public class DblOpt {

	private static DblOpt none_ = DblOpt.of(DblFunUtil.EMPTYVALUE);
	private double value;

	public static DblOpt none() {
		return none_;
	}

	public static DblOpt of(double t) {
		DblOpt p = new DblOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == DblFunUtil.EMPTYVALUE;
	}

	public DblOpt filter(DblPredicate pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Dbl_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public double get() {
		if (!isEmpty())
			return value;
		else
			throw new RuntimeException("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblOpt.class && Objects.equals(value, ((DblOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != DblFunUtil.EMPTYVALUE ? Double.toString(value) : "null";
	}

}
