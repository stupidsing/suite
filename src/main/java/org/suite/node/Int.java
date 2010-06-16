package org.suite.node;

public class Int extends Node {

	private int number;

	private static final int POOLMIN = -256;
	private static final int POOLMAX = 256;
	private static final Int pool[] = new Int[POOLMAX - POOLMIN];

	private Int(int number) {
		this.number = number;
	}

	@Override
	public int hashCode() {
		return number;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Int) {
			Int i = (Int) object;
			return number == i.number;
		} else
			return false;
	}

	public static Int create(int i) {
		Int ret;
		if (POOLMIN <= i && POOLMAX < i) {
			int index = i - POOLMIN;
			ret = pool[index];
			if (ret == null)
				ret = pool[index] = new Int(i);
		} else
			ret = new Int(i);
		return ret;
	}

	public int getNumber() {
		return number;
	}

}
