package suite.immutable;

public class IHashSet<V> {

	private IIntMap<IList<V>> intMap;

	public static <V> IHashSet<V> merge(IHashSet<V> set0, IHashSet<V> set1) {
		return new IHashSet<>(IIntMap.merge(set0.intMap, set1.intMap));
	}

	public IHashSet() {
		this(new IIntMap<>());
	}

	private IHashSet(IIntMap<IList<V>> intMap) {
		this.intMap = intMap;
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
