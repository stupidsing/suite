package suite.primitive;

public class LngFun {

	@FunctionalInterface
	public interface LngObj_Obj<X, Y> {
		public Y apply(long c, X x);

		public default LngObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					throw new RuntimeException("for " + x + ":" + y + ", ", ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface Lng_Obj<T> {
		public T apply(long c);

		public default Lng_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					throw new RuntimeException("for " + i, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface Obj_Lng<T> {
		public long apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Lng<X, Y> {
		public long apply(X x, Y y);
	}

}
