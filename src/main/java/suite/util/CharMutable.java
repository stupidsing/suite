package suite.util;

/**
 * An indirect reference to a primitive character. Character.MIN_VALUE is not
 * allowed in the value.
 * 
 * @author ywsing
 */
public class CharMutable {

	private static char falseValue = 0;
	private static char trueValue = 1;

	private char value;

	public static CharMutable false_() {
		return of(falseValue);
	}

	public static CharMutable true_() {
		return of(trueValue);
	}

	public static CharMutable nil() {
		return CharMutable.of(Character.MIN_VALUE);
	}

	public static CharMutable of(char i) {
		CharMutable p = new CharMutable();
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

	public int get() {
		return value;
	}

}
