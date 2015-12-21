package suite.node;

import java.util.Objects;

import suite.util.Util;

public class Data<T> extends Node {

	public final T data;

	public Data(T data) {
		this.data = data;
	}

	public static <T> T get(Node node) {
		@SuppressWarnings("unchecked")
		T t = (T) ((Data<?>) node).data;
		return t;
	}

	@Override
	public String toString() {
		return "Data [" + data + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(data);
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == Data.class && Objects.equals(data, ((Data<?>) object).data);
	}

}
