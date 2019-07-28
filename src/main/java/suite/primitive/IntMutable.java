package suite.primitive;

import static suite.util.Fail.fail;

import suite.object.Object_;

/**
 * An indirect reference to a primitive int. Integer.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class IntMutable {

	private static int empty = IntFunUtil.EMPTYVALUE;

	private int value;

	public static IntMutable nil() {
		return IntMutable.of(empty);
	}

	public static IntMutable of(int c) {
		var p = new IntMutable();
		p.update(c);
		return p;
	}

	public int increment() {
		return value++;
	}

	public void set(int c) {
		if (value == empty)
			update(c);
		else
			fail("value already set");
	}

	public void update(int c) {
		value = c;
	}

	public int value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IntMutable.class && value == ((IntMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Integer.toString(value) : "null";
	}

}
