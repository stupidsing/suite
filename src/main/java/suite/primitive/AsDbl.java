package suite.primitive;

import java.util.List;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.primitive.DblPrim.ObjObj_Dbl;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.puller.DblObjPuller;
import primal.puller.Puller;
import primal.puller.Puller2;
import suite.adt.map.ListMultimap;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.primitive.streamlet.DblStreamlet;

public class AsDbl {

	public static Doubles build(Sink<DoublesBuilder> sink) {
		var sb = new DoublesBuilder();
		sink.f(sb);
		return sb.toDoubles();
	}

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

	public static <V> DblObjStreamlet<V> read2(DblObjMap<V> map) {
		return new DblObjStreamlet<>(() -> DblObjPuller.of(map.source()));
	}

	public static <V> DblObjStreamlet<List<V>> read2(ListMultimap<Double, V> multimap) {
		return new DblObjStreamlet<>(() -> {
			var iter = multimap.listEntries().iterator();
			return DblObjPuller.of(pair -> {
				var b = iter.hasNext();
				if (b) {
					var pair1 = iter.next();
					pair.update(pair1.k, pair1.v);
				}
				return b;
			});
		});
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

}
