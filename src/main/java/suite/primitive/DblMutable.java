package suite.primitive;

/**
 * An indirect reference to a primitive doubleacter. Double.MIN_VALUE is not
 * allowed in the value.
 * 
 * @author ywsing
 */
public class DblMutable {

	private static double falseValue = 0;
	private static double trueValue = 1;

	private double value;

	public static DblMutable false_() {
		return of(falseValue);
	}

	public static DblMutable true_() {
		return of(trueValue);
	}

	public static DblMutable nil() {
		return DblMutable.of(Double.MIN_VALUE);
	}

	public static DblMutable of(double i) {
		DblMutable p = new DblMutable();
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

	public void set(double t) {
		if (value == Integer.MIN_VALUE)
			update(t);
		else
			throw new RuntimeException("value already set");
	}

	public void update(double t) {
		value = t;
	}

	public double get() {
		return value;
	}

}
