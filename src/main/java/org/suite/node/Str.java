package org.suite.node;

import org.util.Util;

public class Str extends Node {

	private String value;

	public Str(String name) {
		this.value = name;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Str) {
			Str atom = (Str) object;
			return Util.equals(value, atom.value);
		} else
			return false;
	}

	public String getValue() {
		return value;
	}

}
