package suite.util;

public class Pair<T1, T2> {

	public T1 t1;
	public T2 t2;

	public Pair() {
	}

	public Pair(T1 t1, T2 t2) {
		this.t1 = t1;
		this.t2 = t2;
	}

	public static <T1, T2> Pair<T1, T2> create(T1 t1, T2 t2) {
		return new Pair<>(t1, t2);
	}

	public boolean equals(Object o) {
		if (o instanceof Pair<?, ?>) {
			Pair<?, ?> t = (Pair<?, ?>) o;
			return Util.equals(t1, t.t1) && Util.equals(t2, t.t2);
		} else
			return false;
	}

	public int hashCode() {
		int h1 = t1 != null ? t1.hashCode() : 0;
		int h2 = t2 != null ? t2.hashCode() : 0;
		return h1 ^ h2;
	}

	public String toString() {
		return t1.toString() + ":" + t2.toString();
	}

}
