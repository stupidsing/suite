package suite.primitive;

import java.util.List;

import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.primitive.IntPrim.ObjObj_Int;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.fp.IntFunUtil;
import primal.primitive.puller.IntObjPuller;
import primal.primitive.puller.IntPuller;
import primal.puller.Puller;
import primal.puller.Puller2;
import suite.adt.map.ListMultimap;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Read;

public class AsInt {

	public static Ints build(Sink<IntsBuilder> sink) {
		var sb = new IntsBuilder();
		sink.f(sb);
		return sb.toInts();
	}

	@SafeVarargs
	public static <T> IntStreamlet concat(IntStreamlet... streamlets) {
		return new IntStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return IntPuller.of(IntFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

	public static <T> Fun<Puller<T>, IntStreamlet> lift(Obj_Int<T> fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			T t;
			while ((t = ts.pull()) != null)
				b.append(fun1.apply(t));
			return b.toInts().streamlet();
		};
	}

	public static <V> IntObjStreamlet<V> read2(IntObjMap<V> map) {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(map.source()));
	}

	public static <V> IntObjStreamlet<List<V>> read2(ListMultimap<Integer, V> multimap) {
		return new IntObjStreamlet<>(() -> {
			var iter = multimap.listEntries().iterator();
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

	public static <T> Obj_Int<Puller<T>> sum(Obj_Int<T> fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			T t;
			var result = (int) 0;
			while ((t = source.g()) != null)
				result += fun1.apply(t);
			return result;
		};
	}

	public static <K, V> Obj_Int<Puller2<K, V>> sum(ObjObj_Int<K, V> fun0) {
		ObjObj_Int<K, V> fun1 = fun0.rethrow();
		return puller -> {
			var pair = Pair.<K, V> of(null, null);
			var source = puller.source();
			var result = (int) 0;
			while (source.source2(pair))
				result += fun1.apply(pair.k, pair.v);
			return result;
		};
	}

}
