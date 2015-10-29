package suite.immutable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import suite.adt.Pair;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.fs.KeyValueStoreMutator;
import suite.streamlet.Streamlet;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class LazyIbTreeMutator<K, V> implements KeyValueStoreMutator<K, V> {

	private SerializedPageFile<List<Integer>> superblockFile;
	private LazyIbTreePersister<Pair<K, V>> persister;
	private List<Integer> pointers;

	public LazyIbTreeMutator( //
			PageFile pageFile //
			, Comparator<K> keyComparator //
			, Serializer<K> keySerializer //
			, Serializer<V> valueSerializer) {
		PageFile pf0 = new SubPageFileImpl(pageFile, 0, 1);
		PageFile pf1 = new SubPageFileImpl(pageFile, 1, Integer.MAX_VALUE);

		superblockFile = new SerializedPageFileImpl<>(pf0, SerializeUtil.list(SerializeUtil.intSerializer));

		persister = new LazyIbTreePersister<>(pf1 //
				, (p0, p1) -> keyComparator.compare(p0.t0, p1.t0) //
				, SerializeUtil.pair(keySerializer, valueSerializer));

		pointers = superblockFile.load(0);
	}

	@Override
	public Streamlet<K> keys(K start, K end) {
		return persister.load(pointers).stream(node(start), node(end)).map(node -> node.t0);
	}

	@Override
	public V get(K key) {
		List<V> values = new ArrayList<>();
		persister.load(pointers).update(node(key), pair -> {
			values.add(pair.t1);
			return pair;
		});
		return values.get(0);
	}

	@Override
	public void put(K key, V value) {
		Pair<K, V> pair1 = Pair.of(key, value);
		pointers = persister.save(persister.load(pointers).update(node(key), pair0 -> pair1));
	}

	@Override
	public void remove(K key) {
		pointers = persister.save(persister.load(pointers).update(node(key), pair -> null));
	}

	@Override
	public void end(boolean isComplete) {
		superblockFile.save(0, pointers);
		persister.gc(pointers, 9);
	}

	private Pair<K, V> node(K key) {
		return Pair.of(key, null);
	}

}
