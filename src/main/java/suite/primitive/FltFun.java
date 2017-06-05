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
		T apply(float c);
	}

	@FunctionalInterface
	public interface Obj_Flt<T> {
		float apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Flt<X, Y> {
		float apply(X x, Y y);
	}

}
