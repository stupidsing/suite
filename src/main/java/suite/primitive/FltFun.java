package suite.primitive;

public class FltFun {

	@FunctionalInterface
	public interface FltObj_Obj<X, Y> {
		public Y apply(float c, X x);

		public default FltObj_Obj<X, Y> rethrow() {
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
	public interface Flt_Obj<T> {
		public T apply(float c);

		public default Flt_Obj<T> rethrow() {
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
	public interface Obj_Flt<T> {
		public float apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Flt<X, Y> {
		public float apply(X x, Y y);
	}

}
