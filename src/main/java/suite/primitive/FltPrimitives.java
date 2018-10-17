package suite.primitive;

import static suite.util.Friends.fail;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class FltPrimitives {

	public interface FltComparator {
		int compare(float c0, float c1);
	}

	public interface Flt_Obj<T> {
		public T apply(float c);

		public static <T> Fun<FltOutlet, Streamlet<T>> lift(Flt_Obj<T> fun0) {
			var fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				float c;
				while ((c = s.next()) != FltFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Flt_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return fail("for " + i, ex);
				}
			};
		}
	}

	public interface FltObj_Obj<X, Y> {
		public Y apply(float c, X x);

		public default FltObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return fail("for " + x + ":" + y, ex);
				}
			};
		}
	}

	public interface FltObjPredicate<T> {
		public boolean test(float c, T t);

		public default FltObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					return fail("for " + c + ":" + t, ex);
				}
			};
		}
	}

	public interface FltObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(float c, T t);

		public default FltObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface FltObjSource<T> {
		public boolean source2(FltObjPair<T> pair);
	}

	public interface FltTest {
		public boolean test(float c);

		public default FltTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					return fail("for " + c, ex);
				}
			};
		}
	}

	public interface FltSink {
		public void f(float c);

		public default FltSink rethrow() {
			return t -> {
				try {
					f(t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface FltSource {
		public float g();
	}

	public interface Obj_Flt<T> {
		public float apply(T t);

		public static <T> Fun<Outlet<T>, FltStreamlet> lift(Obj_Flt<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new FloatsBuilder();
				T t;
				while ((t = ts.next()) != null)
					b.append(fun1.apply(t));
				return b.toFloats().streamlet();
			};
		}

		public static <T> Obj_Flt<Outlet<T>> sum(Obj_Flt<T> fun0) {
			var fun1 = fun0.rethrow();
			return outlet -> {
				var source = outlet.source();
				T t;
				var result = (float) 0;
				while ((t = source.g()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Flt<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Flt<X, Y> {
		public float apply(X x, Y y);

		public static <K, V> Obj_Flt<Outlet2<K, V>> sum(ObjObj_Flt<K, V> fun0) {
			ObjObj_Flt<K, V> fun1 = fun0.rethrow();
			return outlet -> {
				var pair = Pair.<K, V> of(null, null);
				var source = outlet.source();
				var result = (float) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.t0, pair.t1);
				return result;
			};
		}

		public default ObjObj_Flt<X, Y> rethrow() {
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
