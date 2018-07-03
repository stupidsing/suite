package suite.primitive;

import suite.object.Object_;
import suite.util.Fail;

/**
 * An indirect reference to a primitive char. Character.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class ChrMutable {

	private char value;

	public static ChrMutable nil() {
		return ChrMutable.of(ChrFunUtil.EMPTYVALUE);
	}

	public static ChrMutable of(char c) {
		var p = new ChrMutable();
		p.update(c);
		return p;
	}

	public char increment() {
		return value++;
	}

	public void set(char c) {
		if (value == ChrFunUtil.EMPTYVALUE)
			update(c);
		else
			Fail.t("value already set");
	}

	public void update(char c) {
		value = c;
	}

	public char get() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ChrMutable.class && value == ((ChrMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(value);
	}

	@Override
	public String toString() {
		return value != ChrFunUtil.EMPTYVALUE ? Character.toString(value) : "null";
	}

}
