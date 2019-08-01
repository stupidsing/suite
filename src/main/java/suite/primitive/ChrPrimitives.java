package suite.primitive;

import static primal.statics.Fail.fail;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;

public class ChrPrimitives {

	public interface Obj_Chr<T> {
		public char apply(T t);

		public static <T> Fun<Puller<T>, ChrStreamlet> lift(Obj_Chr<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new CharsBuilder();
				T t;
				while ((t = ts.pull()) != null)
					b.append(fun1.apply(t));
				return b.toChars().streamlet();
			};
		}

		public static <T> Obj_Chr<Puller<T>> sum(Obj_Chr<T> fun0) {
			var fun1 = fun0.rethrow();
			return puller -> {
				var source = puller.source();
				T t;
				var result = (char) 0;
				while ((t = source.g()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Chr<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Chr<X, Y> {
		public char apply(X x, Y y);

		public static <K, V> Obj_Chr<Puller2<K, V>> sum(ObjObj_Chr<K, V> fun0) {
			ObjObj_Chr<K, V> fun1 = fun0.rethrow();
			return puller -> {
				var pair = Pair.<K, V> of(null, null);
				var source = puller.source();
				var result = (char) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.k, pair.v);
				return result;
			};
		}

		public default ObjObj_Chr<X, Y> rethrow() {
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
