package suite.immutable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import suite.adt.Pair;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.fs.KeyValueStoreMutator;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreeMutator<K, V> implements KeyValueStoreMutator<K, V> {

	private SerializedPageFile<Integer> superblockFile;
	private LazyIbTreePersister<Integer, Pair<K, V>> persister;
	private LazyIbTree<Pair<K, V>> tree;

	public LazyIbTreeMutator(PageFile pageFile, Comparator<K> kc, Serializer<K> ks, Serializer<V> vs) {
		PageFile pf0 = new SubPageFileImpl(pageFile, 0, 1);
		PageFile pf1 = new SubPageFileImpl(pageFile, 1, Integer.MAX_VALUE);
		Comparator<Pair<K, V>> comparator = (p0, p1) -> kc.compare(p0.t0, p1.t0);

		superblockFile = new SerializedPageFileImpl<>(pf0, Serialize.nullable(Serialize.int_));
		persister = new LazyIbTreePageFilePersister<>(pf1, comparator, Serialize.pair(ks, vs));

		Integer pointer = superblockFile.load(0);
		if (pointer == null)
			superblockFile.save(0, pointer = persister.save(new LazyIbTree<>(comparator)));
		tree = persister.load(pointer);
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

	@Override
	public void end(boolean isComplete) {
		if (isComplete) {
			int pointer1 = persister.save(tree);
			int pointerx = persister.gc(Arrays.asList(pointer1), 9).get(pointer1);
			superblockFile.save(0, pointerx);
		}

		try {
			persister.close();
			superblockFile.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
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
