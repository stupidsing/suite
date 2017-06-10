package suite.primitive;

import suite.adt.pair.IntObjPair;

public class IntPrimitives {

	@FunctionalInterface
	public interface Int_Obj<T> {
		public T apply(int c);

		public default Int_Obj<T> rethrow() {
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
	public interface IntObjPredicate<T> {
		public boolean test(int c, T t);

		public default IntObjPredicate<T> rethrow() {
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
	public interface IntObjSink<T> { // extends ObjIntConsumer<T>
		public void sink2(int c, T t);

		public default IntObjSink<T> rethrow() {
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
	public interface IntObjSource<T> {
		public boolean source2(IntObjPair<T> pair);
	}

	@FunctionalInterface
	public interface IntPredicate_ {
		public boolean test(int c);

		public default IntPredicate_ rethrow() {
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
	public interface IntSink_ {
		public void sink(int c);

		public default IntSink_ rethrow() {
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
	public interface IntSource_ {
		public int source();
	}

	@FunctionalInterface
	public interface Obj_Int<T> {
		public int apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Int<X, Y> {
		public int apply(X x, Y y);
	}

}
