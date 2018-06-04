package suite.util;

public abstract class BaseObject<T extends BaseObject<T>> implements Comparable<T>, AutoInterface<T> {

	@Override
	public int compareTo(T t1) {
		return objectSupport().compare(self(), t1);
	}

	@Override
	public boolean equals(Object object) {
		return objectSupport().isEquals(self(), object);
	}

	@Override
	public int hashCode() {
		return objectSupport().hashCode(self());
	}

	@Override
	public String toString() {
		return objectSupport().toString(self());
	}

	protected abstract ObjectSupport<T> objectSupport();

	private T self() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return t;
	}

}
