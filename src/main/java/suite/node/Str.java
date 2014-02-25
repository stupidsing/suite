package suite.node;

import java.util.Objects;

import suite.util.Util;

public class Str extends Node {

	private String value;

	public Str(String name) {
		value = name;
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
				return Objects.equals(value, str.value);
			} else
				return false;
		} else
			return false;
	}

	public String getValue() {
		return value;
	}

}
