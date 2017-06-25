package suite.primitive;

/**
 * An indirect reference to a primitive int. Integer.MIN_VALUE is not allowed in
 * the value.
 * 
 * @author ywsing
 */
public class IntMutable {

	private int value;

	public static IntMutable nil() {
		return IntMutable.of(IntFunUtil.EMPTYVALUE);
	}

	public static IntMutable of(int i) {
		IntMutable p = new IntMutable();
		p.update(i);
		return p;
	}

	public int increment() {
		return value++;
	}

	public void set(int t) {
		if (value == IntFunUtil.EMPTYVALUE)
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
