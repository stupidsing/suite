package suite.primitive;

import static suite.util.Fail.fail;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntPuller;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class IntPrimitives {

	public interface IntComparator {
		int compare(int c0, int c1);
	}

	public interface Int_Obj<T> {
		public T apply(int c);

		public static <T> Fun<IntPuller, Streamlet<T>> lift(Int_Obj<T> fun0) {
			var fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				int c;
				while ((c = s.pull()) != IntFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Int_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return fail("for " + i, ex);
				}
			};
		}
	}

	public interface IntObj_Obj<X, Y> {
		public Y apply(int c, X x);

		public default IntObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return fail("for " + x + ":" + y, ex);
				}
			};
		}
	}

	public interface IntObjPredicate<T> {
		public boolean test(int c, T t);

		public default IntObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					return fail("for " + c + ":" + t, ex);
				}
			};
		}
	}

	public interface IntObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(int c, T t);

		public default IntObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface IntObjSource<T> {
		public boolean source2(IntObjPair<T> pair);
	}

	public interface IntTest {
		public boolean test(int c);

		public default IntTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					return fail("for " + c, ex);
				}
			};
		}
	}

	public interface IntSink {
		public void f(int c);

		public default IntSink rethrow() {
			return t -> {
				try {
					f(t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface IntSource {
		public int g();
	}

	public interface Obj_Int<T> {
		public int apply(T t);

		public static <T> Fun<Puller<T>, IntStreamlet> lift(Obj_Int<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new IntsBuilder();
				T t;
				while ((t = ts.pull()) != null)
					b.append(fun1.apply(t));
				return b.toInts().streamlet();
			};
		}

		public static <T> Obj_Int<Puller<T>> sum(Obj_Int<T> fun0) {
			var fun1 = fun0.rethrow();
			return puller -> {
				var source = puller.source();
				T t;
				var result = (int) 0;
				while ((t = source.g()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Int<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Int<X, Y> {
		public int apply(X x, Y y);

		public static <K, V> Obj_Int<Puller2<K, V>> sum(ObjObj_Int<K, V> fun0) {
			ObjObj_Int<K, V> fun1 = fun0.rethrow();
			return puller -> {
				var pair = Pair.<K, V> of(null, null);
				var source = puller.source();
				var result = (int) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.k, pair.v);
				return result;
			};
		}

		public default ObjObj_Int<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return fail("for " + x + ":" + y, ex);
				}
			};
		}
	}

}
