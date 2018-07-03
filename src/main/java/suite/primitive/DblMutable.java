package suite.primitive;

import suite.object.Object_;
import suite.util.Fail;

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

	public static DblMutable of(double c) {
		var p = new DblMutable();
		p.update(c);
		return p;
	}

	public double increment() {
		return value++;
	}

	public void set(double c) {
		if (value == DblFunUtil.EMPTYVALUE)
			update(c);
		else
			Fail.t("value already set");
	}

	public void update(double c) {
		value = c;
	}

	public double get() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblMutable.class && value == ((DblMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public String toString() {
		return value != DblFunUtil.EMPTYVALUE ? Double.toString(value) : "null";
	}

}
