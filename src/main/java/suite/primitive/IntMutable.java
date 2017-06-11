package suite.primitive;

/**
 * An indirect reference to a primitive int. Integer.MIN_VALUE is not allowed in
 * the value.
 * 
 * @author ywsing
 */
public class IntMutable {

	private static int falseValue = 0;
	private static int trueValue = 1;

	private int value;

	public static IntMutable false_() {
		return of(falseValue);
	}

	public static IntMutable true_() {
		return of(trueValue);
	}

	public static IntMutable nil() {
		return IntMutable.of(Integer.MIN_VALUE);
	}

	public static IntMutable of(int i) {
		IntMutable p = new IntMutable();
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

	public void set(int t) {
		if (value == Integer.MIN_VALUE)
			update(t);
		else
			throw new RuntimeException("value already set");
	}

	public void update(int t) {
		value = t;
	}

	public int get() {
		return value;
	}

}
