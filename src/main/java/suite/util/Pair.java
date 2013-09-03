package suite.util;

public class Pair<T0, T1> {

	public T0 t0;
	public T1 t1;

	public Pair() {
	}

	public Pair(T0 t1, T1 t2) {
		this.t0 = t1;
		this.t1 = t2;
	}

	public static <T0, T1> Pair<T0, T1> create(T0 t0, T1 t1) {
		return new Pair<>(t0, t1);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Pair<?, ?>) {
			Pair<?, ?> other = (Pair<?, ?>) object;
			return Util.equals(t0, other.t0) && Util.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int h1 = t0 != null ? t0.hashCode() : 0;
		int h2 = t1 != null ? t1.hashCode() : 0;
		return h1 ^ h2;
	}

	@Override
	public String toString() {
		return t0.toString() + ":" + t1.toString();
	}

}
