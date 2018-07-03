package suite.adt;

import java.util.Objects;

import suite.object.Object_;
import suite.util.Fail;

public class Mutable<T> {

	private T value;

	public static <T> Mutable<T> nil() {
		return Mutable.of(null);
	}

	public static <T> Mutable<T> of(T t) {
		var p = new Mutable<T>();
		p.update(t);
		return p;
	}

	public void set(T t) {
		if (value == null)
			update(t);
		else
			Fail.t("value already set");
	}

	public void update(T t) {
		value = t;
	}

	public T get() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Mutable.class && Objects.equals(value, ((Mutable<?>) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != null ? value.toString() : "null";
	}

}
