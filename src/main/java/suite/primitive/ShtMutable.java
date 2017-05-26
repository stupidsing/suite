package suite.primitive;

/**
 * An indirect reference to a primitive shortacter. Short.MIN_VALUE is not
 * allowed in the value.
 * 
 * @author ywsing
 */
public class ShtMutable {

	private static short falseValue = 0;
	private static short trueValue = 1;

	private short value;

	public static ShtMutable false_() {
		return of(falseValue);
	}

	public static ShtMutable true_() {
		return of(trueValue);
	}

	public static ShtMutable nil() {
		return ShtMutable.of(Short.MIN_VALUE);
	}

	public static ShtMutable of(short i) {
		ShtMutable p = new ShtMutable();
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

	public void set(short t) {
		if (value == Integer.MIN_VALUE)
			update(t);
		else
			throw new RuntimeException("value already set");
	}

	public void update(short t) {
		value = t;
	}

	public short get() {
		return value;
	}

}
