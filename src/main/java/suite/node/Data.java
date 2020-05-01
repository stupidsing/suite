package suite.node;

import primal.Verbs.Equals;
import primal.Verbs.Get;

import java.util.Objects;

public class Data<T> extends Node {

	public final T data;

	public Data(T data) {
		this.data = data;
	}

	public static <T> T get(Node node) {
		@SuppressWarnings("unchecked")
		var t = (T) ((Data<?>) node).data;
		return t;
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == Data.class && Equals.ab(data, ((Data<?>) object).data);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(data);
	}

	@Override
	public String toString() {
		return "Data [" + data + "]";
	}

}
