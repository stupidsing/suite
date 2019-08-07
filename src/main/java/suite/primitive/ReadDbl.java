package suite.primitive;

import java.util.List;

import primal.adt.map.ListMultimap;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.adt.Doubles.DoublesBuilder;
import primal.primitive.adt.map.DblObjMap;
import primal.primitive.adt.map.ObjDblMap;
import primal.primitive.fp.DblFunUtil;
import primal.primitive.puller.DblObjPuller;
import primal.primitive.puller.DblPuller;
import primal.puller.Puller;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.Read;

public class ReadDbl {

	@SafeVarargs
	public static <T> DblStreamlet concat(DblStreamlet... streamlets) {
		return new DblStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return DblPuller.of(DblFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

	public static <V> DblObjStreamlet<V> from2(DblObjMap<V> map) {
		return new DblObjStreamlet<>(() -> DblObjPuller.of(map.source()));
	}

	public static <V> DblObjStreamlet<V> from2(ObjDblMap<V> map) {
		return new DblObjStreamlet<>(() -> DblObjPuller.of(map.source()));
	}

	public static <V> DblObjStreamlet<V> from2(Source<DblObjPuller<V>> puller) {
		return new DblObjStreamlet<>(puller);
	}

	public static <T> Fun<Puller<T>, DblStreamlet> lift(Obj_Dbl<T> fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			T t;
			while ((t = ts.pull()) != null)
				b.append(fun1.apply(t));
			return new DblStreamlet(b.toDoubles()::puller);
		};
	}

	public static <V> DblObjStreamlet<V> read2(DblObjMap<V> map) {
		return new DblObjStreamlet<>(() -> DblObjPuller.of(map.source()));
	}

	public static <V> DblObjStreamlet<List<V>> read2(ListMultimap<Double, V> multimap) {
		return new DblObjStreamlet<>(() -> {
			var iter = Read.listEntries(multimap).iterator();
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

}
