package suite.primitive;

import static suite.util.Friends.fail;

import java.util.Objects;

import suite.adt.Opt;
import suite.object.Object_;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.IntPrimitives.Int_Obj;

public class IntOpt {

	private static IntOpt none_ = IntOpt.of(IntFunUtil.EMPTYVALUE);
	private int value;

	public static IntOpt none() {
		return none_;
	}

	public static IntOpt of(int t) {
		var p = new IntOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == IntFunUtil.EMPTYVALUE;
	}

	public IntOpt filter(IntTest pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Int_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public int get() {
		return !isEmpty() ? value : fail("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IntOpt.class && Objects.equals(value, ((IntOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != IntFunUtil.EMPTYVALUE ? Integer.toString(value) : "null";
	}

}
