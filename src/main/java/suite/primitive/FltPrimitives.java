package suite.primitive;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;

public class FltPrimitives {

	@FunctionalInterface
	public interface FltComparator {
		int compare(float c0, float c1);
	}

	@FunctionalInterface
	public interface Flt_Obj<T> {
		public T apply(float c);

		public static <T> Fun<FltOutlet, Streamlet<T>> lift(Flt_Obj<T> fun0) {
			Flt_Obj<T> fun1 = fun0.rethrow();
			return s -> {
				List<T> ts = new ArrayList<>();
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
					throw new RuntimeException("for " + x + ":" + y, ex);
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
					throw new RuntimeException("for " + c + ":" + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface FltObjSink<T> { // extends ObjCharConsumer<T>
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
	public interface FltTest {
		public boolean test(float c);

		public default FltTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					throw new RuntimeException("for " + c, ex);
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

		public static <T> Fun<Outlet<T>, FltStreamlet> lift(Obj_Flt<T> fun0) {
			Obj_Flt<T> fun1 = fun0.rethrow();
			return ts -> {
				FloatsBuilder b = new FloatsBuilder();
				T t;
				while ((t = ts.next()) != null)
					b.append(fun1.apply(t));
				return b.toFloats().streamlet();
			};
		}

		public static <T> Obj_Flt<Outlet<T>> sum(Obj_Flt<T> fun0) {
			Obj_Flt<T> fun1 = fun0.rethrow();
			return outlet -> {
				Source<T> source = outlet.source();
				T t;
				float result = (float) 0;
				while ((t = source.source()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Flt<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface ObjObj_Flt<X, Y> {
		public float apply(X x, Y y);

		public static <K, V> Obj_Flt<Outlet2<K, V>> sum(ObjObj_Flt<K, V> fun0) {
			ObjObj_Flt<K, V> fun1 = fun0.rethrow();
			return outlet -> {
				Pair<K, V> pair = Pair.of(null, null);
				Source2<K, V> source = outlet.source();
				float result = (float) 0;
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
					throw new RuntimeException("for " + x + ":" + y, ex);
				}
			};
		}
	}

}
