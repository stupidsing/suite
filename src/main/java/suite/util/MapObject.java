package suite.util;

public abstract class MapObject<T extends MapObject<T>> implements Cloneable, Comparable<T>, MapInterface<T> {

	@Override
	public MapObject<T> clone() {
		return MapObject_.clone(self());
	}

	@Override
	public int compareTo(T other) {
		return autoObject().compare(self(), other);
	}

	@Override
	public boolean equals(Object object) {
		var t0 = self();
		boolean b;
		if (t0.getClass() == object.getClass()) {
			@SuppressWarnings("unchecked")
			var t1 = (T) object;
			b = autoObject().equals(t0, t1);
		} else
			b = false;
		return b;
	}

	@Override
	public int hashCode() {
		return autoObject().hashCode(self());
	}

	@Override
	public String toString() {
		return autoObject().toString(self());
	}

	private AutoObject_<Object> autoObject() {
		return new AutoObject_<>(MapObject_::list);
	}

	private T self() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return t;
	}

}
