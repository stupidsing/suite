package suite.primitive;

import static suite.util.Friends.fail;

import java.util.Objects;

import suite.adt.Opt;
import suite.object.Object_;
import suite.primitive.LngPrimitives.LngTest;
import suite.primitive.LngPrimitives.Lng_Obj;

public class LngOpt {

	private static LngOpt none_ = LngOpt.of(LngFunUtil.EMPTYVALUE);
	private long value;

	public static LngOpt none() {
		return none_;
	}

	public static LngOpt of(long t) {
		var p = new LngOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == LngFunUtil.EMPTYVALUE;
	}

	public LngOpt filter(LngTest pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Lng_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public long get() {
		return !isEmpty() ? value : fail("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == LngOpt.class && Objects.equals(value, ((LngOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != LngFunUtil.EMPTYVALUE ? Long.toString(value) : "null";
	}

}
