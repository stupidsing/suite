package suite.persistent;

import primal.fp.Funs2.BinOp;
import primal.persistent.PerList;
import primal.streamlet.Streamlet;

public class PerHashSet<V> {

	private PerIntMap<PerList<V>> intMap;

	public static <V> PerHashSet<V> meld(PerHashSet<V> set0, PerHashSet<V> set1) {
		return meld(set0, set1, (v0, v1) -> v0);
	}

	public static <V> PerHashSet<V> meld(PerHashSet<V> set0, PerHashSet<V> set1, BinOp<V> f) {
		return new PerHashSet<>(PerIntMap.meld(set0.intMap, set1.intMap, (l0, l1) -> {
			var list = PerList.<V> end();
			for (var v : l0)
				list = merge(list, v, f);
			for (var v : l1)
				list = merge(list, v, f);
			return list;
		}));
	}

	private static <V> PerList<V> merge(PerList<V> list0, V v0, BinOp<V> f) {
		var list1 = PerList.<V> end();
		for (var v : list0)
			if (!v.equals(v0))
				list1 = PerList.cons(v, list1);
			else
				v0 = f.apply(v0, v);
		return PerList.cons(v0, list1);
	}

	public PerHashSet() {
		this(new PerIntMap<>());
	}

	private PerHashSet(PerIntMap<PerList<V>> intMap) {
		this.intMap = intMap;
	}

	public Streamlet<V> streamlet() {
		return intMap.streamlet().flatMap(iterable -> iterable);
	}

	public PerList<V> get(int hashCode) {
		return intMap.get(hashCode);
	}

	public boolean contains(V v) {
		return intMap.get(v.hashCode()).contains(v);
	}

	public PerHashSet<V> add(V v) {
		return new PerHashSet<>(intMap.update(v.hashCode(), list -> !list.contains(v) ? PerList.cons(v, list) : list));
	}

	public PerHashSet<V> remove(V v) {
		return new PerHashSet<>(intMap.update(v.hashCode(), list -> {
			var list1 = list.remove(v);
			return !list1.isEmpty() ? list1 : null;
		}));
	}

}
