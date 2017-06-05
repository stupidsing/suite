package suite.primitive;

public class IntFun {

	@FunctionalInterface
	public interface IntObj_Obj<X, Y> {
		public Y apply(int c, X x);

		public default IntObj_Obj<X, Y> rethrow() {
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
	public interface Int_Obj<T> {
		T apply(int c);
	}

	@FunctionalInterface
	public interface Obj_Int<T> {
		int apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Int<X, Y> {
		int apply(X x, Y y);
	}

}
