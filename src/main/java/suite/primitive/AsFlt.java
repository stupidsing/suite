package suite.primitive;

import java.util.List;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.FltPrim.ObjObj_Flt;
import primal.primitive.FltPrim.Obj_Flt;
import primal.puller.Puller;
import primal.puller.Puller2;
import suite.adt.map.ListMultimap;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.streamlet.FltObjPuller;
import suite.primitive.streamlet.FltStreamlet;

public class AsFlt {

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

	public static <V> FltObjPuller<V> read2(FltObjMap<V> map) {
		return FltObjPuller.of(map.source());
	}

	public static <V> FltObjPuller<List<V>> read2(ListMultimap<Float, V> multimap) {
		var iter = multimap.listEntries().iterator();
		return FltObjPuller.of(pair -> {
			var b = iter.hasNext();
			if (b) {
				var pair1 = iter.next();
				pair.update(pair1.k, pair1.v);
			}
			return b;
		});
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

}
