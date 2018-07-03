package suite.immutable;

import java.util.Objects;

import suite.adt.pair.Pair;
import suite.streamlet.FunUtil2.BinOp;
import suite.streamlet.FunUtil2.Source2;
import suite.streamlet.Outlet2;
import suite.streamlet.Streamlet2;

public class IHashMap<K, V> {

	private IHashSet<Pair<K, V>> set;

	public static <K, V> IHashMap<K, V> meld(IHashMap<K, V> map0, IHashMap<K, V> map1, BinOp<V> f) {
		return new IHashMap<>(IHashSet.meld(map0.set, map1.set, (e0, e1) -> Pair.of(e0.t0, f.apply(e0.t1, e1.t1))));
	}

	public IHashMap() {
		this(new IHashSet<>());
	}

	public IHashMap(IHashSet<Pair<K, V>> set) {
		this.set = set;
	}

	public Streamlet2<K, V> streamlet() {
		return new Streamlet2<>(() -> {
			var source = set.streamlet().source();
			return Outlet2.of(new Source2<K, V>() {
				public boolean source2(Pair<K, V> pair) {
					var pair1 = source.source();
					if (pair1 != null) {
						pair.update(pair1.t0, pair1.t1);
						return true;
					} else
						return false;
				}
			});
		});
	}

	public V get(K key) {
		for (var e : set.get(key.hashCode()))
			if (Objects.equals(key, e.t0))
				return e.t1;
		return null;
	}

	public IHashMap<K, V> add(K k, V v) {
		return new IHashMap<>(set.add(Pair.of(k, v)));
	}

	public IHashMap<K, V> remove(K k) {
		return new IHashMap<>(set.remove(Pair.of(k, null)));
	}

}
