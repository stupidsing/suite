package suite.immutable;

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
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class LazyIbTreeMutator<K, V> implements KeyValueStoreMutator<K, V> {

	private SerializedPageFile<Integer> superblockFile;
	private LazyIbTreePersister<Pair<K, V>> persister;
	private Integer pointer;

	public LazyIbTreeMutator(PageFile pageFile, Comparator<K> kc, Serializer<K> ks, Serializer<V> vs) {
		PageFile pf0 = new SubPageFileImpl(pageFile, 0, 1);
		PageFile pf1 = new SubPageFileImpl(pageFile, 1, Integer.MAX_VALUE);
		Comparator<Pair<K, V>> comparator = (p0, p1) -> kc.compare(p0.t0, p1.t0);
		Source<Integer> source = () -> persister.save(new LazyIbTree<>(comparator));

		superblockFile = new SerializedPageFileImpl<>(pf0, SerializeUtil.intSerializer, source);
		persister = new LazyIbTreePersister<>(pf1, comparator, SerializeUtil.pair(ks, vs));
		pointer = superblockFile.load(0);
	}

	@Override
	public Streamlet<K> keys(K start, K end) {
		return persister.load(pointer).stream(node(start), node(end)).map(Pair::first_);
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
		superblockFile.save(0, pointer);
		persister.gc(Arrays.asList(pointer), 9);
	}

	private synchronized void update(K key, Fun<Pair<K, V>, Pair<K, V>> fun) {
		pointer = update0(key, fun);
	}

	private Integer update0(K key, Fun<Pair<K, V>, Pair<K, V>> fun) {
		LazyIbTree<Pair<K, V>> tree0 = persister.load(pointer);
		LazyIbTree<Pair<K, V>> treex = tree0.update(node(key), fun);
		return persister.save(treex);
	}

	private Pair<K, V> node(K key) {
		return Pair.of(key, null);
	}

}
