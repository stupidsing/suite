package suite.primitive;

/**
 * An indirect reference to a primitive long. Long.MIN_VALUE is not allowed in
 * the value.
 * 
 * @author ywsing
 */
public class LngMutable {

	private long value;

	public static LngMutable nil() {
		return LngMutable.of(LngFunUtil.EMPTYVALUE);
	}

	public static LngMutable of(long i) {
		LngMutable p = new LngMutable();
		p.update(i);
		return p;
	}

	public long increment() {
		return value++;
	}

	public void set(long t) {
		if (value == LngFunUtil.EMPTYVALUE)
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
