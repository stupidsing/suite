package suite.primitive;

import static suite.util.Fail.fail;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngPuller;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class LngPrimitives {

	public interface LngComparator {
		int compare(long c0, long c1);
	}

	public interface Lng_Obj<T> {
		public T apply(long c);

		public static <T> Fun<LngPuller, Streamlet<T>> lift(Lng_Obj<T> fun0) {
			var fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				long c;
				while ((c = s.pull()) != LngFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Lng_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return fail("for " + i, ex);
				}
			};
		}
	}

	public interface LngObj_Obj<X, Y> {
		public Y apply(long c, X x);

		public default LngObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return fail("for " + x + ":" + y, ex);
				}
			};
		}
	}

	public interface LngObjPredicate<T> {
		public boolean test(long c, T t);

		public default LngObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					return fail("for " + c + ":" + t, ex);
				}
			};
		}
	}

	public interface LngObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(long c, T t);

		public default LngObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface LngObjSource<T> {
		public boolean source2(LngObjPair<T> pair);
	}

	public interface LngTest {
		public boolean test(long c);

		public default LngTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					return fail("for " + c, ex);
				}
			};
		}
	}

	public interface LngSink {
		public void f(long c);

		public default LngSink rethrow() {
			return t -> {
				try {
					f(t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface LngSource {
		public long g();
	}

	public interface Obj_Lng<T> {
		public long apply(T t);

		public static <T> Fun<Puller<T>, LngStreamlet> lift(Obj_Lng<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new LongsBuilder();
				T t;
				while ((t = ts.pull()) != null)
					b.append(fun1.apply(t));
				return b.toLongs().streamlet();
			};
		}

		public static <T> Obj_Lng<Puller<T>> sum(Obj_Lng<T> fun0) {
			var fun1 = fun0.rethrow();
			return puller -> {
				var source = puller.source();
				T t;
				var result = (long) 0;
				while ((t = source.g()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Lng<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Lng<X, Y> {
		public long apply(X x, Y y);

		public static <K, V> Obj_Lng<Puller2<K, V>> sum(ObjObj_Lng<K, V> fun0) {
			ObjObj_Lng<K, V> fun1 = fun0.rethrow();
			return puller -> {
				var pair = Pair.<K, V> of(null, null);
				var source = puller.source();
				var result = (long) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.k, pair.v);
				return result;
			};
		}

		public default ObjObj_Lng<X, Y> rethrow() {
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
