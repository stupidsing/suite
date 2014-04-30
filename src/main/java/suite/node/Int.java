package suite.node;

import suite.util.Util;

public class Int extends Node {

	private int number;

	private static int poolLo = -256;
	private static int poolHi = 256;
	private static Int pool[] = new Int[poolHi - poolLo];

	private Int(int number) {
		this.number = number;
	}

	public static Int create(int i) {
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

	@Override
	public int hashCode() {
		return number;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			if (Util.clazz(node) == Int.class) {
				Int i = (Int) node;
				return number == i.number;
			} else
				return false;
		} else
			return false;
	}

	public int getNumber() {
		return number;
	}

}
