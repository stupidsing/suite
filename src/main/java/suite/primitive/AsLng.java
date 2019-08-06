package suite.primitive;

import java.util.List;

import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.primitive.LngPrim.ObjObj_Lng;
import primal.primitive.LngPrim.Obj_Lng;
import primal.primitive.fp.LngFunUtil;
import primal.primitive.puller.LngObjPuller;
import primal.primitive.puller.LngPuller;
import primal.puller.Puller;
import primal.puller.Puller2;
import suite.adt.map.ListMultimap;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Read;

public class AsLng {

	public static Longs build(Sink<LongsBuilder> sink) {
		var sb = new LongsBuilder();
		sink.f(sb);
		return sb.toLongs();
	}

	@SafeVarargs
	public static <T> LngStreamlet concat(LngStreamlet... streamlets) {
		return new LngStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return LngPuller.of(LngFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

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

	public static <V> LngObjStreamlet<V> read2(LngObjMap<V> map) {
		return new LngObjStreamlet<>(() -> LngObjPuller.of(map.source()));
	}

	public static <V> LngObjStreamlet<List<V>> read2(ListMultimap<Long, V> multimap) {
		return new LngObjStreamlet<>(() -> {
			var iter = multimap.listEntries().iterator();
			return LngObjPuller.of(pair -> {
				var b = iter.hasNext();
				if (b) {
					var pair1 = iter.next();
					pair.update(pair1.k, pair1.v);
				}
				return b;
			});
		});
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
