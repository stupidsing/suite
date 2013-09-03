package suite.fp;

import java.util.Comparator;

import suite.util.Pair;
import suite.util.Util;

public class ImmutableRbTreeMap<K extends Comparable<K>, V> extends ImmutableRbTree<Pair<K, V>> {

	public ImmutableRbTreeMap() {
		super(new Comparator<Pair<K, V>>() {
			public int compare(Pair<K, V> p0, Pair<K, V> p1) {
				K k0 = p0 != null ? p0.t0 : null;
				K k1 = p1 != null ? p1.t0 : null;
				return Util.compare(k0, k1);
			}
		});
	}

	public V get(K k) {
		Pair<K, V> pair = find(Pair.create(k, (V) null));
		return pair != null ? pair.t1 : null;
	}

	public ImmutableRbTree<Pair<K, V>> put(K k, V v) {
		return add(Pair.create(k, v));
	}

}
