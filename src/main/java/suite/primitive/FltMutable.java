package suite.primitive;

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

}
