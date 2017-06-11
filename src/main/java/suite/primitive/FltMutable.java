package suite.primitive;

/**
 * An indirect reference to a primitive float. Float.MIN_VALUE is not allowed in
 * the value.
 * 
 * @author ywsing
 */
public class FltMutable {

	private static float falseValue = 0;
	private static float trueValue = 1;

	private float value;

	public static FltMutable false_() {
		return of(falseValue);
	}

	public static FltMutable true_() {
		return of(trueValue);
	}

	public static FltMutable nil() {
		return FltMutable.of(Float.MIN_VALUE);
	}

	public static FltMutable of(float i) {
		FltMutable p = new FltMutable();
		p.update(i);
		return p;
	}

	public boolean isTrue() {
		return value == trueValue;
	}

	public void setFalse() {
		update(falseValue);
	}

	public void setTrue() {
		update(trueValue);
	}

	public void set(float t) {
		if (value == Integer.MIN_VALUE)
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
