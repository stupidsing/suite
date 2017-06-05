package suite.primitive;

public class ChrFun {

	@FunctionalInterface
	public interface ChrObj_Obj<X, Y> {
		public Y apply(char c, X x);

		public default ChrObj_Obj<X, Y> rethrow() {
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
	public interface Chr_Obj<T> {
		T apply(char c);
	}

	@FunctionalInterface
	public interface Obj_Chr<T> {
		char apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Chr<X, Y> {
		char apply(X x, Y y);
	}

}
