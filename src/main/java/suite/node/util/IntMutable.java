package suite.node.util;

/**
 * An indirect reference to a primitive integer. Integer.MIN_VALUE is not
 * allowed in the value.
 * 
 * @author ywsing
 */
public class IntMutable {

	private static int trueValue = 1;
	private int value;

	public static IntMutable false_() {
		return of(0);
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

	public void setTrue() {
		set(trueValue);
	}

	public void set(int t) {
		if (value == Integer.MIN_VALUE)
			update(t);
		else
			throw new RuntimeException("Value already set");
	}

	public void update(int t) {
		value = t;
	}

	public int get() {
		return value;
	}

}
