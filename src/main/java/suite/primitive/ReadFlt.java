package suite.primitive;

import java.util.List;

import primal.adt.map.ListMultimap;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.FltPrim.Obj_Flt;
import primal.primitive.adt.Floats.FloatsBuilder;
import primal.primitive.adt.map.FltObjMap;
import primal.primitive.adt.map.ObjFltMap;
import primal.primitive.fp.FltFunUtil;
import primal.primitive.puller.FltObjPuller;
import primal.primitive.puller.FltPuller;
import primal.puller.Puller;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.Read;

public class ReadFlt {

	@SafeVarargs
	public static <T> FltStreamlet concat(FltStreamlet... streamlets) {
		return new FltStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return FltPuller.of(FltFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

	public static <V> FltObjStreamlet<V> from2(FltObjMap<V> map) {
		return new FltObjStreamlet<>(() -> FltObjPuller.of(map.source()));
	}

	public static <V> FltObjStreamlet<V> from2(ObjFltMap<V> map) {
		return new FltObjStreamlet<>(() -> FltObjPuller.of(map.source()));
	}

	public static <V> FltObjStreamlet<V> from2(Source<FltObjPuller<V>> puller) {
		return new FltObjStreamlet<>(puller);
	}

	public static <T> Fun<Puller<T>, FltStreamlet> lift(Obj_Flt<T> fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			T t;
			while ((t = ts.pull()) != null)
				b.append(fun1.apply(t));
			return new FltStreamlet(b.toFloats()::puller);
		};
	}

	public static <V> FltObjStreamlet<V> read2(FltObjMap<V> map) {
		return new FltObjStreamlet<>(() -> FltObjPuller.of(map.source()));
	}

	public static <V> FltObjStreamlet<List<V>> read2(ListMultimap<Float, V> multimap) {
		return new FltObjStreamlet<>(() -> {
			var iter = Read.listEntries(multimap).iterator();
			return FltObjPuller.of(pair -> {
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
