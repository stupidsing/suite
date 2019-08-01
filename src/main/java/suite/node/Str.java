package suite.node;

import primal.String_;
import primal.Verbs.Get;

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
		return Get.clazz(object) == Str.class ? String_.equals(value, ((Str) object).value) : false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
