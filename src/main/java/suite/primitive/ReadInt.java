package suite.primitive;

import java.util.List;

import primal.adt.map.ListMultimap;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.adt.Ints.IntsBuilder;
import primal.primitive.adt.map.IntObjMap;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.fp.IntFunUtil;
import primal.primitive.puller.IntObjPuller;
import primal.primitive.puller.IntPuller;
import primal.puller.Puller;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Read;

public class ReadInt {

	@SafeVarargs
	public static <T> IntStreamlet concat(IntStreamlet... streamlets) {
		return new IntStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return IntPuller.of(IntFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

	public static <V> IntObjStreamlet<V> from2(IntObjMap<V> map) {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(map.source()));
	}

	public static <V> IntObjStreamlet<V> from2(ObjIntMap<V> map) {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(map.source()));
	}

	public static <V> IntObjStreamlet<V> from2(Source<IntObjPuller<V>> puller) {
		return new IntObjStreamlet<>(puller);
	}

	public static <T> Fun<Puller<T>, IntStreamlet> lift(Obj_Int<T> fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			T t;
			while ((t = ts.pull()) != null)
				b.append(fun1.apply(t));
			return new IntStreamlet(b.toInts()::puller);
		};
	}

	public static <V> IntObjStreamlet<V> read2(IntObjMap<V> map) {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(map.source()));
	}

	public static <V> IntObjStreamlet<List<V>> read2(ListMultimap<Integer, V> multimap) {
		return new IntObjStreamlet<>(() -> {
			var iter = Read.listEntries(multimap).iterator();
			return IntObjPuller.of(pair -> {
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
