package suite.immutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import suite.adt.Pair;
import suite.file.SerializedPageFile;
import suite.fs.KeyValueStoreMutator;
import suite.streamlet.Streamlet;

public class LazyIbTreeMutator<Pointer, K, V> implements KeyValueStoreMutator<K, V> {

	private SerializedPageFile<Pointer> superblockFile;
	private LazyIbTreePersister<Pointer, Pair<K, V>> persister;
	private LazyIbTreeStore<K, V> store;

	public LazyIbTreeMutator( //
			SerializedPageFile<Pointer> superblockFile, //
			LazyIbTreePersister<Pointer, Pair<K, V>> persister, //
			Comparator<K> kc) {
		this.superblockFile = superblockFile;
		this.persister = persister;

		Pointer pointer = superblockFile.load(0);
		if (pointer == null)
			superblockFile.save(0, pointer = persister.save(new LazyIbTree<>((p0, p1) -> kc.compare(p0.t0, p1.t0))));
		store = new LazyIbTreeStore<>(persister.load(pointer));
	}

	@Override
	public Streamlet<K> keys(K start, K end) {
		return store.keys(start, end);
	}

	@Override
	public V get(K key) {
		return store.get(key);
	}

	@Override
	public void put(K key, V value) {
		store.put(key, value);
	}

	@Override
	public void remove(K key) {
		store.remove(key);
	}

	@Override
	public synchronized void end(boolean isComplete) {
		if (isComplete) {
			Pointer pointer1 = persister.save(store.get());
			Pointer pointerx = persister.gc(Arrays.asList(pointer1), 9).get(pointer1);
			superblockFile.save(0, pointerx);
		}

		try {
			persister.close();
			superblockFile.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
