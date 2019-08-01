package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.ArrayList;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.FltPrim;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.streamlet.FltPuller;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class FltPrimitives {

	public interface Flt_Obj<T> {
		public T apply(float c);

		public static <T> Fun<FltPuller, Streamlet<T>> lift(Flt_Obj<T> fun0) {
			var fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				float c;
				while ((c = s.pull()) != FltPrim.EMPTYVALUE)
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

	public interface Obj_Flt<T> {
		public float apply(T t);

		public static <T> Fun<Puller<T>, FltStreamlet> lift(Obj_Flt<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new FloatsBuilder();
				T t;
				while ((t = ts.pull()) != null)
					b.append(fun1.apply(t));
				return b.toFloats().streamlet();
			};
		}

		public static <T> Obj_Flt<Puller<T>> sum(Obj_Flt<T> fun0) {
			var fun1 = fun0.rethrow();
			return puller -> {
				var source = puller.source();
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

		public static <K, V> Obj_Flt<Puller2<K, V>> sum(ObjObj_Flt<K, V> fun0) {
			ObjObj_Flt<K, V> fun1 = fun0.rethrow();
			return puller -> {
				var pair = Pair.<K, V> of(null, null);
				var source = puller.source();
				var result = (float) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.k, pair.v);
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
