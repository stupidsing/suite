package suite.primitive;

import suite.adt.pair.DblObjPair;

public class DblPrimitives {

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
	public interface DblObjPredicate<T> {
		public boolean test(double c, T t);

		public default DblObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + c + ":" + t + ", ", ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface DblObjSink<T> { // extends ObjDoubleConsumer<T>
		public void sink2(double c, T t);

		public default DblObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface DblObjSource<T> {
		public boolean source2(DblObjPair<T> pair);
	}

	@FunctionalInterface
	public interface DblPredicate {
		public boolean test(double c);

		public default DblPredicate rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					throw new RuntimeException("for " + c + ", ", ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface DblSink {
		public void sink(double c);

		public default DblSink rethrow() {
			return t -> {
				try {
					sink(t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface DblSource {
		public double source();
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
