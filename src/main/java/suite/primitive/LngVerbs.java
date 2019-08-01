package suite.primitive;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.LngPrim.ObjObj_Lng;
import primal.primitive.LngPrim.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;

public class LngVerbs {

	public static class AsLng {
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
	}

}
