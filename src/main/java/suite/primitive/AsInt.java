package suite.primitive;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.IntPrim.ObjObj_Int;
import primal.primitive.IntPrim.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;

public class AsInt {

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
