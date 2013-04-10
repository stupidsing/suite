package org.suite.node;

import org.util.Util;

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
			if (node instanceof Str) {
				Str str = (Str) node;
				return Util.equals(value, str.value);
			} else
				return false;
		} else
			return false;
	}

	public String getValue() {
		return value;
	}

}
