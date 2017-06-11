package suite.primitive;

/**
 * An indirect reference to a primitive char. Character.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class ChrMutable {

	private static char falseValue = 0;
	private static char trueValue = 1;

	private char value;

	public static ChrMutable false_() {
		return of(falseValue);
	}

	public static ChrMutable true_() {
		return of(trueValue);
	}

	public static ChrMutable nil() {
		return ChrMutable.of(Character.MIN_VALUE);
	}

	public static ChrMutable of(char i) {
		ChrMutable p = new ChrMutable();
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

	public void set(char t) {
		if (value == Integer.MIN_VALUE)
			update(t);
		else
			throw new RuntimeException("value already set");
	}

	public void update(char t) {
		value = t;
	}

	public char get() {
		return value;
	}

}
