package suite.persistent;

import primal.Verbs.Equals;
import primal.adt.Pair;
import primal.fp.Funs2.BinOp;
import primal.fp.Funs2.Pair_;
import primal.fp.Funs2.Source2;
import primal.puller.Puller2;
import primal.streamlet.Streamlet2;

public class PerHashMap<K, V> {

	private PerHashSet<Pair<K, V>> set;

	public static <K, V> PerHashMap<K, V> meld(PerHashMap<K, V> map0, PerHashMap<K, V> map1, BinOp<V> f) {
		return new PerHashMap<>(PerHashSet.meld(map0.set, map1.set, (e0, e1) -> Pair.of(e0.k, f.apply(e0.v, e1.v))));
	}

	public PerHashMap() {
		this(new PerHashSet<>());
	}

	public PerHashMap(PerHashSet<Pair<K, V>> set) {
		this.set = set;
	}

	public Streamlet2<K, V> streamlet() {
		return new Streamlet2<>(() -> {
			var source = set.streamlet().source();
			return Puller2.of(new Source2<K, V>() {
				public boolean source2(Pair_<K, V> pair) {
					var pair1 = source.g();
					if (pair1 != null) {
						pair.update(pair1.k, pair1.v);
						return true;
					} else
						return false;
				}
			});
		});
	}

	public V get(K key) {
		for (var e : set.get(key.hashCode()))
			if (Equals.ab(key, e.k))
				return e.v;
		return null;
	}

	public PerHashMap<K, V> add(K k, V v) {
		return new PerHashMap<>(set.add(Pair.of(k, v)));
	}

	public PerHashMap<K, V> remove(K k) {
		return new PerHashMap<>(set.remove(Pair.of(k, null)));
	}

}
