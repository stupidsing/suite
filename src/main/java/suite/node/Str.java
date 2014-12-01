package suite.node;

import suite.util.Util;

public class Str extends Node {

	public final String value;

	public Str(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			if (Util.clazz(node) == Str.class) {
				Str str = (Str) node;
				return Util.stringEquals(value, str.value);
			} else
				return false;
		} else
			return false;
	}

}
