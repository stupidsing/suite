package suite.node;

import suite.util.Object_;
import suite.util.String_;

public class Str extends Node {

	public final String value;

	public Str(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Str.class) {
			Str str = (Str) object;
			return String_.equals(value, str.value);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
