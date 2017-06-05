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
		T apply(double c);
	}

	@FunctionalInterface
	public interface Obj_Dbl<T> {
		double apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Dbl<X, Y> {
		double apply(X x, Y y);
	}

}
