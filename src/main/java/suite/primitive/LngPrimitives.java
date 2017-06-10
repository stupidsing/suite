package suite.primitive;

import suite.adt.pair.LngObjPair;

public class LngPrimitives {

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
	public interface LngObjPredicate<T> {
		public boolean test(long c, T t);

		public default LngObjPredicate<T> rethrow() {
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
	public interface LngObjSink<T> { // extends ObjLongConsumer<T>
		public void sink2(long c, T t);

		public default LngObjSink<T> rethrow() {
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
	public interface LngObjSource<T> {
		public boolean source2(LngObjPair<T> pair);
	}

	@FunctionalInterface
	public interface LngPredicate {
		public boolean test(long c);

		public default LngPredicate rethrow() {
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
	public interface LngSink {
		public void sink(long c);

		public default LngSink rethrow() {
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
	public interface LngSource {
		public long source();
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
