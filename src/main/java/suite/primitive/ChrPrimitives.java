package suite.primitive;

import suite.adt.pair.ChrObjPair;

public class ChrPrimitives {

	@FunctionalInterface
	public interface Chr_Obj<T> {
		public T apply(char c);

		public default Chr_Obj<T> rethrow() {
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
	public interface ChrObjPredicate<T> {
		public boolean test(char c, T t);

		public default ChrObjPredicate<T> rethrow() {
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
	public interface ChrObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(char c, T t);

		public default ChrObjSink<T> rethrow() {
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
	public interface ChrObjSource<T> {
		public boolean source2(ChrObjPair<T> pair);
	}

	@FunctionalInterface
	public interface ChrPredicate_ {
		public boolean test(char c);

		public default ChrPredicate_ rethrow() {
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
	public interface ChrSink_ {
		public void sink(char c);

		public default ChrSink_ rethrow() {
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
	public interface ChrSource_ {
		public char source();
	}

	@FunctionalInterface
	public interface Obj_Chr<T> {
		public char apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Chr<X, Y> {
		public char apply(X x, Y y);
	}

}
