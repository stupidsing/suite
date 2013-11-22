package suite.fp;

import java.util.Comparator;
import java.util.Iterator;

import suite.util.Pair;
import suite.util.Util;

public class ImmutableRbTreeMap<K extends Comparable<K>, V> {

	private ImmutableRbTree<Pair<K, V>> tree = new ImmutableRbTree<Pair<K, V>>(new Comparator<Pair<K, V>>() {
		public int compare(Pair<K, V> p0, Pair<K, V> p1) {
			K k0 = p0 != null ? p0.t0 : null;
			K k1 = p1 != null ? p1.t0 : null;
			return Util.compare(k0, k1);
		}
	});

	public ImmutableRbTreeMap() {
	}

	public ImmutableRbTreeMap(ImmutableRbTree<Pair<K, V>> tree) {
		this.tree = tree;
	}

	public V get(K k) {
		Pair<K, V> pair = tree.find(Pair.create(k, (V) null));
		return pair != null ? pair.t1 : null;
	}

	public Iterator<Pair<K, V>> iterator() {
		return tree.iterator();
	}

	public ImmutableRbTreeMap<K, V> put(K k, V v) {
		return new ImmutableRbTreeMap<K, V>(tree.add(Pair.create(k, v)));
	}

	public ImmutableRbTreeMap<K, V> replace(K k, V v) {
		return new ImmutableRbTreeMap<K, V>(tree.replace(Pair.create(k, v)));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");

		for (Pair<K, V> pair : Util.iter(tree.iterator()))
			sb.append(pair.t0 + " = " + pair.t1 + ", ");

		sb.append("}");
		return sb.toString();
	}

}
