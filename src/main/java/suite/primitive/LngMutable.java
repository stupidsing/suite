package suite.primitive;

/**
 * An indirect reference to a primitive longacter. Long.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class LngMutable {

	private static long falseValue = 0;
	private static long trueValue = 1;

	private long value;

	public static LngMutable false_() {
		return of(falseValue);
	}

	public static LngMutable true_() {
		return of(trueValue);
	}

	public static LngMutable nil() {
		return LngMutable.of(Long.MIN_VALUE);
	}

	public static LngMutable of(long i) {
		LngMutable p = new LngMutable();
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

	public void set(long t) {
		if (value == Integer.MIN_VALUE)
			update(t);
		else
			throw new RuntimeException("value already set");
	}

	public void update(long t) {
		value = t;
	}

	public long get() {
		return value;
	}

}
