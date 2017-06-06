package suite.primitive;

public class DblFun {

	@FunctionalInterface
	public interface DblObj_Obj<X, Y> {
		public Y apply(double c, X x);

		public default DblObj_Obj<X, Y> rethrow() {
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
	public interface Dbl_Obj<T> {
		public T apply(double c);

		public default Dbl_Obj<T> rethrow() {
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
	public interface Obj_Dbl<T> {
		public double apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Dbl<X, Y> {
		public double apply(X x, Y y);
	}

}
