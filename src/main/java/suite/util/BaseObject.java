package suite.util;

public abstract class BaseObject<T extends BaseObject<T>> implements Cloneable, Comparable<T>, AutoInterface<T> {

	@Override
	public int compareTo(T t1) {
		return autoObject().compare(self(), t1);
	}

	@Override
	public boolean equals(Object object) {
		return autoObject().isEquals(self(), object);
	}

	@Override
	public int hashCode() {
		return autoObject().hashCode(self());
	}

	@Override
	public String toString() {
		return autoObject().toString(self());
	}

	protected abstract AutoObject_<T> autoObject();

}
