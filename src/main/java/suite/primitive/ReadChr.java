package suite.primitive;

import java.util.List;

import primal.adt.map.ListMultimap;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.ChrPrim.Obj_Chr;
import primal.primitive.adt.Chars.CharsBuilder;
import primal.primitive.adt.map.ChrObjMap;
import primal.primitive.adt.map.ObjChrMap;
import primal.primitive.fp.ChrFunUtil;
import primal.primitive.puller.ChrObjPuller;
import primal.primitive.puller.ChrPuller;
import primal.puller.Puller;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.Read;

public class ReadChr {

	@SafeVarargs
	public static <T> ChrStreamlet concat(ChrStreamlet... streamlets) {
		return new ChrStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return ChrPuller.of(ChrFunUtil.concat(FunUtil.map(st -> st.puller().source(), source)));
		});
	}

	public static <V> ChrObjStreamlet<V> from2(ChrObjMap<V> map) {
		return new ChrObjStreamlet<>(() -> ChrObjPuller.of(map.source()));
	}

	public static <V> ChrObjStreamlet<V> from2(ObjChrMap<V> map) {
		return new ChrObjStreamlet<>(() -> ChrObjPuller.of(map.source()));
	}

	public static <V> ChrObjStreamlet<V> from2(Source<ChrObjPuller<V>> puller) {
		return new ChrObjStreamlet<>(puller);
	}

	public static <T> Fun<Puller<T>, ChrStreamlet> lift(Obj_Chr<T> fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			T t;
			while ((t = ts.pull()) != null)
				b.append(fun1.apply(t));
			return new ChrStreamlet(b.toChars()::puller);
		};
	}

	public static <V> ChrObjStreamlet<V> read2(ChrObjMap<V> map) {
		return new ChrObjStreamlet<>(() -> ChrObjPuller.of(map.source()));
	}

	public static <V> ChrObjStreamlet<List<V>> read2(ListMultimap<Character, V> multimap) {
		return new ChrObjStreamlet<>(() -> {
			var iter = Read.listEntries(multimap).iterator();
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

}
