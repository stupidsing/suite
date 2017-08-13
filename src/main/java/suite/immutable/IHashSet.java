package suite.immutable;

import suite.streamlet.Streamlet;
import suite.util.FunUtil2.BinOp;

public class IHashSet<V> {

	private IIntMap<IList<V>> intMap;

	public static <V> IHashSet<V> meld(IHashSet<V> set0, IHashSet<V> set1) {
		return meld(set0, set1, (v0, v1) -> v0);
	}

	public static <V> IHashSet<V> meld(IHashSet<V> set0, IHashSet<V> set1, BinOp<V> f) {
		return new IHashSet<>(IIntMap.meld(set0.intMap, set1.intMap, (l0, l1) -> {
			IList<V> list = IList.end();
			for (V v : l0)
				list = merge(list, v, f);
			for (V v : l1)
				list = merge(list, v, f);
			return list;
		}));
	}

	private static <V> IList<V> merge(IList<V> list0, V v0, BinOp<V> f) {
		IList<V> list1 = IList.end();
		for (V v : list0)
			if (!v.equals(v0))
				list1 = IList.cons(v, list1);
			else
				v0 = f.apply(v0, v);
		return IList.cons(v0, list1);
	}

	public IHashSet() {
		this(new IIntMap<>());
	}

	private IHashSet(IIntMap<IList<V>> intMap) {
		this.intMap = intMap;
	}

	public Streamlet<V> streamlet() {
		return intMap.streamlet().flatMap(iterable -> iterable);
	}

	public IList<V> get(int hashCode) {
		return intMap.get(hashCode);
	}

	public boolean contains(V v) {
		return intMap.get(v.hashCode()).contains(v);
	}

	public IHashSet<V> add(V v) {
		return new IHashSet<>(intMap.update(v.hashCode(), list -> !list.contains(v) ? IList.cons(v, list) : list));
	}

	public IHashSet<V> remove(V v) {
		return new IHashSet<>(intMap.update(v.hashCode(), list -> {
			IList<V> list1 = list.remove(v);
			return !list1.isEmpty() ? list1 : null;
		}));
	}

}
