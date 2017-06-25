package suite.primitive;

/**
 * An indirect reference to a primitive double. Double.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class DblMutable {

	private double value;

	public static DblMutable nil() {
		return DblMutable.of(DblFunUtil.EMPTYVALUE);
	}

	public static DblMutable of(double i) {
		DblMutable p = new DblMutable();
		p.update(i);
		return p;
	}

	public double increment() {
		return value++;
	}

	public void set(double t) {
		if (value == DblFunUtil.EMPTYVALUE)
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
