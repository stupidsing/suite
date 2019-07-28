package suite.primitive;

import static suite.util.Fail.fail;

import suite.object.Object_;

/**
 * An indirect reference to a primitive long. Long.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class LngMutable {

	private static long empty = LngFunUtil.EMPTYVALUE;

	private long value;

	public static LngMutable nil() {
		return LngMutable.of(empty);
	}

	public static LngMutable of(long c) {
		var p = new LngMutable();
		p.update(c);
		return p;
	}

	public long increment() {
		return value++;
	}

	public void set(long c) {
		if (value == empty)
			update(c);
		else
			fail("value already set");
	}

	public void update(long c) {
		value = c;
	}

	public long value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == LngMutable.class && value == ((LngMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Long.toString(value) : "null";
	}

}
