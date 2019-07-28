package suite.primitive;

import static suite.util.Fail.fail;

import suite.object.Object_;

/**
 * An indirect reference to a primitive char. Character.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class ChrMutable {

	private static char empty = ChrFunUtil.EMPTYVALUE;

	private char value;

	public static ChrMutable nil() {
		return ChrMutable.of(empty);
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
		if (value == empty)
			update(c);
		else
			fail("value already set");
	}

	public void update(char c) {
		value = c;
	}

	public char value() {
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
		return value != empty ? Character.toString(value) : "null";
	}

}
