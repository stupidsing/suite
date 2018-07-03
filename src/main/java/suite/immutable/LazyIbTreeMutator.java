package suite.immutable;

import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.fs.KeyValueMutator;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.Streamlet;

public class LazyIbTreeMutator<K, V> implements KeyValueMutator<K, V> {

	private LazyIbTree<Pair<K, V>> tree;

	public LazyIbTreeMutator(LazyIbTree<Pair<K, V>> tree) {
		this.tree = tree;
	}

	@Override
	public Streamlet<K> keys(K start, K end) {
		return tree.stream(node(start), node(end)).map(Pair::fst);
	}

	@Override
	public V get(K key) {
		var mutable = Mutable.<V> nil();
		update_(key, pair -> {
			mutable.set(pair != null ? pair.t1 : null);
			return pair;
		});
		return mutable.get();
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

	private synchronized void update(K key, Iterate<Pair<K, V>> fun) {
		tree = update_(key, fun);
	}

	private LazyIbTree<Pair<K, V>> update_(K key, Iterate<Pair<K, V>> fun) {
		return tree.update(node(key), fun);
	}

	private Pair<K, V> node(K key) {
		return Pair.of(key, null);
	}

}
