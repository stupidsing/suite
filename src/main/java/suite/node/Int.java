package suite.node;

import suite.util.Util;

public class Int extends Node {

	public final int number;

	private static int poolLo = -256;
	private static int poolHi = 256;
	private static Int pool[] = new Int[poolHi - poolLo];

	public static Int of(int i) {
		Int ret;
		if (poolLo <= i && i < poolHi) {
			int index = i - poolLo;
			ret = pool[index];
			if (ret == null)
				ret = pool[index] = new Int(i);
		} else
			ret = new Int(i);
		return ret;
	}

	private Int(int number) {
		this.number = number;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Int.class) {
			Int i = (Int) object;
			return number == i.number;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return number;
	}

}
