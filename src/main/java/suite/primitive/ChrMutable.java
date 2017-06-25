package suite.primitive;

/**
 * An indirect reference to a primitive char. Character.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class ChrMutable {

	private char value;

	public static ChrMutable nil() {
		return ChrMutable.of(ChrFunUtil.EMPTYVALUE);
	}

	public static ChrMutable of(char i) {
		ChrMutable p = new ChrMutable();
		p.update(i);
		return p;
	}

	public char increment() {
		return value++;
	}

	public void set(char t) {
		if (value == ChrFunUtil.EMPTYVALUE)
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
