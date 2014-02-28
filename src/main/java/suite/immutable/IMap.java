package suite.immutable;

import java.util.Iterator;

import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.Pair;

public class IMap<K extends Comparable<K>, V> implements Iterable<Pair<K, V>> {

	private ITree<Pair<K, V>> tree = new I23Tree<Pair<K, V>>(Pair.<K, V> comparatorByFirst());

	public IMap() {
	}

	public IMap(ITree<Pair<K, V>> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return FunUtil.iterator(source());
	}

	public Source<Pair<K, V>> source() {
		return tree.source();
	}

	public V get(K k) {
		return Pair.second(tree.find(Pair.create(k, (V) null)));
	}

	public IMap<K, V> put(K k, V v) {
		return new IMap<K, V>(tree.add(Pair.create(k, v)));
	}

	public IMap<K, V> replace(K k, V v) {
		return new IMap<K, V>(tree.replace(Pair.create(k, v)));
	}

	public IMap<K, V> remove(K k) {
		return new IMap<K, V>(tree.remove(Pair.create(k, (V) null)));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");

		for (Pair<K, V> pair : this)
			sb.append(pair.t0 + " = " + pair.t1 + ", ");

		sb.append("}");
		return sb.toString();
	}

}
