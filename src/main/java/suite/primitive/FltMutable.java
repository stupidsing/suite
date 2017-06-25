package suite.primitive;

import suite.util.Object_;

/**
 * An indirect reference to a primitive float. Float.MIN_VALUE is not allowed in
 * the value.
 * 
 * @author ywsing
 */
public class FltMutable {

	private float value;

	public static FltMutable nil() {
		return FltMutable.of(FltFunUtil.EMPTYVALUE);
	}

	public static FltMutable of(float i) {
		FltMutable p = new FltMutable();
		p.update(i);
		return p;
	}

	public float increment() {
		return value++;
	}

	public void set(float t) {
		if (value == FltFunUtil.EMPTYVALUE)
			update(t);
		else
			throw new RuntimeException("value already set");
	}

	public void update(float t) {
		value = t;
	}

	public float get() {
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
		return value != FltFunUtil.EMPTYVALUE ? Float.toString(value) : "null";
	}

}
