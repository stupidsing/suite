package suite.primitive;

import java.util.List;

import primal.adt.map.ListMultimap;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.LngPrim.Obj_Lng;
import primal.primitive.adt.Longs.LongsBuilder;
import primal.primitive.adt.map.LngObjMap;
import primal.primitive.adt.map.ObjLngMap;
import primal.primitive.fp.LngFunUtil;
import primal.primitive.puller.LngObjPuller;
import primal.primitive.puller.LngPuller;
import primal.puller.Puller;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Read;

public class ReadLng {

	@SafeVarargs
	public static <T> LngStreamlet concat(LngStreamlet... streamlets) {
		return new LngStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return LngPuller.of(LngFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

	public static <V> LngObjStreamlet<V> from2(LngObjMap<V> map) {
		return new LngObjStreamlet<>(() -> LngObjPuller.of(map.source()));
	}

	public static <V> LngObjStreamlet<V> from2(ObjLngMap<V> map) {
		return new LngObjStreamlet<>(() -> LngObjPuller.of(map.source()));
	}

	public static <V> LngObjStreamlet<V> from2(Source<LngObjPuller<V>> puller) {
		return new LngObjStreamlet<>(puller);
	}

	public static <T> Fun<Puller<T>, LngStreamlet> lift(Obj_Lng<T> fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			T t;
			while ((t = ts.pull()) != null)
				b.append(fun1.apply(t));
			return new LngStreamlet(b.toLongs()::puller);
		};
	}

	public static <V> LngObjStreamlet<V> read2(LngObjMap<V> map) {
		return new LngObjStreamlet<>(() -> LngObjPuller.of(map.source()));
	}

	public static <V> LngObjStreamlet<List<V>> read2(ListMultimap<Long, V> multimap) {
		return new LngObjStreamlet<>(() -> {
			var iter = Read.listEntries(multimap).iterator();
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

}
