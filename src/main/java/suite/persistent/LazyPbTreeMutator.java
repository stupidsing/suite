package suite.persistent;

import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.Funs.Iterate;
import suite.fs.KeyValueMutator;
import suite.streamlet.Streamlet;

public class LazyPbTreeMutator<K, V> implements KeyValueMutator<K, V> {

	private LazyPbTree<Pair<K, V>> tree;

	public LazyPbTreeMutator(LazyPbTree<Pair<K, V>> tree) {
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
			mutable.set(pair != null ? pair.v : null);
			return pair;
		});
		return mutable.value();
	}

	@Override
	public void put(K key, V value) {
		update(key, pair0 -> Pair.of(key, value));
	}

	@Override
	public void remove(K key) {
		update(key, pair0 -> null);
	}

	public LazyPbTree<Pair<K, V>> get() {
		return tree;
	}

	private synchronized void update(K key, Iterate<Pair<K, V>> fun) {
		tree = update_(key, fun);
	}

	private LazyPbTree<Pair<K, V>> update_(K key, Iterate<Pair<K, V>> fun) {
		return tree.update(node(key), fun);
	}

	private Pair<K, V> node(K key) {
		return Pair.of(key, null);
	}

}
