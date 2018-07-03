package suite.node;

import suite.object.Object_;
import suite.util.String_;

public class Str extends Node {

	public final String value;

	public static String str(Node node) {
		return ((Str) node).value;
	}

	public Str(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Str.class ? String_.equals(value, ((Str) object).value) : false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
