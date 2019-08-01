package suite.primitive;

import static primal.statics.Fail.fail;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;

public class DblPrimitives {

	public interface Obj_Dbl<T> {
		public double apply(T t);

		public static <T> Fun<Puller<T>, DblStreamlet> lift(Obj_Dbl<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new DoublesBuilder();
				T t;
				while ((t = ts.pull()) != null)
					b.append(fun1.apply(t));
				return b.toDoubles().streamlet();
			};
		}

		public static <T> Obj_Dbl<Puller<T>> sum(Obj_Dbl<T> fun0) {
			var fun1 = fun0.rethrow();
			return puller -> {
				var source = puller.source();
				T t;
				var result = (double) 0;
				while ((t = source.g()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Dbl<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Dbl<X, Y> {
		public double apply(X x, Y y);

		public static <K, V> Obj_Dbl<Puller2<K, V>> sum(ObjObj_Dbl<K, V> fun0) {
			ObjObj_Dbl<K, V> fun1 = fun0.rethrow();
			return puller -> {
				var pair = Pair.<K, V> of(null, null);
				var source = puller.source();
				var result = (double) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.k, pair.v);
				return result;
			};
		}

		public default ObjObj_Dbl<X, Y> rethrow() {
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
