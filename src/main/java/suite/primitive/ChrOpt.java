package suite.primitive;

import static suite.util.Fail.fail;

import java.util.Objects;

import suite.adt.Opt;
import suite.object.Object_;
import suite.primitive.ChrPrimitives.ChrTest;
import suite.primitive.ChrPrimitives.Chr_Obj;

public class ChrOpt {

	private static char empty = ChrFunUtil.EMPTYVALUE;
	private static ChrOpt none_ = ChrOpt.of(empty);

	private char value;

	public static ChrOpt none() {
		return none_;
	}

	public static ChrOpt of(char t) {
		var p = new ChrOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == empty;
	}

	public ChrOpt filter(ChrTest pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Chr_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public char get() {
		return !isEmpty() ? value : fail("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ChrOpt.class && Objects.equals(value, ((ChrOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Character.toString(value) : "null";
	}

}
