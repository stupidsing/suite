package suite.node;

import java.util.Objects;

import primal.Ob;

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
		return Ob.clazz(object) == Data.class && Ob.equals(data, ((Data<?>) object).data);
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
