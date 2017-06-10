package suite.primitive;

import suite.adt.pair.FltObjPair;

public class FltPrimitives {

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
	public interface FltObjPredicate<T> {
		public boolean test(float c, T t);

		public default FltObjPredicate<T> rethrow() {
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
	public interface FltObjSink<T> { // extends ObjFloatConsumer<T>
		public void sink2(float c, T t);

		public default FltObjSink<T> rethrow() {
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
	public interface FltObjSource<T> {
		public boolean source2(FltObjPair<T> pair);
	}

	@FunctionalInterface
	public interface FltPredicate {
		public boolean test(float c);

		public default FltPredicate rethrow() {
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
	public interface FltSink {
		public void sink(float c);

		public default FltSink rethrow() {
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
	public interface FltSource {
		public float source();
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
