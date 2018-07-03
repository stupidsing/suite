package suite.node;

import suite.object.Object_;

public class Int extends Node {

	public final int number;

	private static int poolLo = -256;
	private static int poolHi = 256;
	private static Int[] pool = new Int[poolHi - poolLo];

	public static int num(Node node) {
		return ((Int) node).number;
	}

	public static Int of(int i) {
		Int ret;
		if (poolLo <= i && i < poolHi) {
			var index = i - poolLo;
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
		if (Object_.clazz(object) == Int.class) {
			var i = (Int) object;
			return number == i.number;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return number;
	}

}
