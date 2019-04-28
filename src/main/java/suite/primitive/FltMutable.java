package suite.primitive;

import static suite.util.Friends.fail;

import suite.object.Object_;

/**
 * An indirect reference to a primitive float. Float.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class FltMutable {

	private static float empty = FltFunUtil.EMPTYVALUE;

	private float value;

	public static FltMutable nil() {
		return FltMutable.of(empty);
	}

	public static FltMutable of(float c) {
		var p = new FltMutable();
		p.update(c);
		return p;
	}

	public float increment() {
		return value++;
	}

	public void set(float c) {
		if (value == empty)
			update(c);
		else
			fail("value already set");
	}

	public void update(float c) {
		value = c;
	}

	public float value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == FltMutable.class && value == ((FltMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(value);
	}

	@Override
	public String toString() {
		return value != empty ? Float.toString(value) : "null";
	}

}
