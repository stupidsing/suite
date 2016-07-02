package suite.node.util;

public class Mutable<T> {

	private T value;

	public static <T> Mutable<T> nil() {
		return Mutable.of(null);
	}

	public static <T> Mutable<T> of(T t) {
		Mutable<T> p = new Mutable<>();
		p.update(t);
		return p;
	}

	public void set(T t) {
		if (value == null)
			update(t);
		else
			throw new RuntimeException("Value already set");
	}

	public void update(T t) {
		value = t;
	}

	public T get() {
		return value;
	}

}
