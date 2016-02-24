package suite.immutable;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Pair;
import suite.fs.KeyValueStore;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class LazyIbTreeStore<K, V> implements KeyValueStore<K, V> {

	private LazyIbTree<Pair<K, V>> tree;

	public LazyIbTreeStore(LazyIbTree<Pair<K, V>> tree) {
		this.tree = tree;
	}

	@Override
	public Streamlet<K> keys(K start, K end) {
		return tree.stream(node(start), node(end)).map(Pair::first_);
	}

	@Override
	public V get(K key) {
		List<V> values = new ArrayList<>();
		update0(key, pair -> {
			values.add(pair != null ? pair.t1 : null);
			return pair;
		});
		return values.get(0);
	}

	@Override
	public void put(K key, V value) {
		update(key, pair0 -> Pair.of(key, value));
	}

	@Override
	public void remove(K key) {
		update(key, pair0 -> null);
	}

	public LazyIbTree<Pair<K, V>> get() {
		return tree;
	}

	private synchronized void update(K key, Fun<Pair<K, V>, Pair<K, V>> fun) {
		tree = update0(key, fun);
	}

	private LazyIbTree<Pair<K, V>> update0(K key, Fun<Pair<K, V>, Pair<K, V>> fun) {
		return tree.update(node(key), fun);
	}

	private Pair<K, V> node(K key) {
		return Pair.of(key, null);
	}

}
