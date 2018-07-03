package suite.node;

import java.util.Objects;

import suite.object.Object_;

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
		return Object_.clazz(object) == Data.class && Objects.equals(data, ((Data<?>) object).data);
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
