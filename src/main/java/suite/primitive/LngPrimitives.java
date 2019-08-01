package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.ArrayList;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.LngPrim;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.LngPuller;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class LngPrimitives {

	public interface Lng_Obj<T> {
		public T apply(long c);

		public static <T> Fun<LngPuller, Streamlet<T>> lift(Lng_Obj<T> fun0) {
			var fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				long c;
				while ((c = s.pull()) != LngPrim.EMPTYVALUE)
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
