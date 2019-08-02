package suite.primitive;

import java.util.List;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.primitive.ChrPrim.ObjObj_Chr;
import primal.primitive.ChrPrim.Obj_Chr;
import primal.primitive.puller.ChrObjPuller;
import primal.puller.Puller;
import primal.puller.Puller2;
import suite.adt.map.ListMultimap;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.primitive.streamlet.ChrStreamlet;

public class AsChr {

	public static Chars build(Sink<CharsBuilder> sink) {
		var sb = new CharsBuilder();
		sink.f(sb);
		return sb.toChars();
	}

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

	public static <V> ChrObjStreamlet<V> read2(ChrObjMap<V> map) {
		return new ChrObjStreamlet<>(() -> ChrObjPuller.of(map.source()));
	}

	public static <V> ChrObjStreamlet<List<V>> read2(ListMultimap<Character, V> multimap) {
		return new ChrObjStreamlet<>(() -> {
			var iter = multimap.listEntries().iterator();
			return ChrObjPuller.of(pair -> {
				var b = iter.hasNext();
				if (b) {
					var pair1 = iter.next();
					pair.update(pair1.k, pair1.v);
				}
				return b;
			});
		});
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

}
