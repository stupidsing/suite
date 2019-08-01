package suite.primitive;

import static primal.statics.Fail.fail;

import primal.primitive.IntPrim;

public class BooMutable {

	private static int falseValue = 0;
	private static int trueValue = 1;

	private int value;

	public static BooMutable false_() {
		return of(falseValue);
	}

	public static BooMutable true_() {
		return of(trueValue);
	}

	public static BooMutable nil() {
		return BooMutable.of(IntPrim.EMPTYVALUE);
	}

	public static BooMutable of(int i) {
		var p = new BooMutable();
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
		if (value == IntPrim.EMPTYVALUE)
			update(t);
		else
			fail("value already set");
	}

	public void update(int t) {
		value = t;
	}

	public int get() {
		return value;
	}

}
