package suite.immutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import suite.adt.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.fs.KeyValueStore;
import suite.fs.KeyValueStoreMutator;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreeMutator<Pointer, Key, Value> implements KeyValueStoreMutator<Key, Value> {

	private SerializedPageFile<Pointer> superblockFile;
	private LazyIbTreePersister<Pointer, Pair<Key, Value>> persister;
	private LazyIbTreeStore<Key, Value> store;

	public static <K, V> LazyIbTreeMutator<Extent, K, V> ofExtent( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		Comparator<Pair<K, V>> pc = (p0, p1) -> kc.compare(p0.t0, p1.t0);
		Serializer<Pair<K, V>> ps = Serialize.pair(ks, vs);
		PageFile pf0 = FileFactory.subPageFile(pageFile, 0, 1);
		PageFile pf1 = FileFactory.subPageFile(pageFile, 1, Integer.MAX_VALUE);
		SerializedPageFile<Extent> superblockFile = SerializedFileFactory.serialized(pf0, Serialize.nullable(Serialize.extent()));
		LazyIbTreePersister<Extent, Pair<K, V>> persister = new LazyIbTreeExtentFilePersister<>(pf1, pc, ps);
		return new LazyIbTreeMutator<>(superblockFile, persister, kc);
	}

	public static <K, V> LazyIbTreeMutator<Integer, K, V> ofPage( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		Comparator<Pair<K, V>> pc = (p0, p1) -> kc.compare(p0.t0, p1.t0);
		Serializer<Pair<K, V>> ps = Serialize.pair(ks, vs);
		PageFile pf0 = FileFactory.subPageFile(pageFile, 0, 1);
		PageFile pf1 = FileFactory.subPageFile(pageFile, 1, Integer.MAX_VALUE);
		SerializedPageFile<Integer> superblockFile = SerializedFileFactory.serialized(pf0, Serialize.nullable(Serialize.int_));
		LazyIbTreePersister<Integer, Pair<K, V>> persister = new LazyIbTreePageFilePersister<>(pf1, pc, ps);
		return new LazyIbTreeMutator<>(superblockFile, persister, kc);
	}

	public LazyIbTreeMutator( //
			SerializedPageFile<Pointer> superblockFile, //
			LazyIbTreePersister<Pointer, Pair<Key, Value>> persister, //
			Comparator<Key> kc) {
		this.superblockFile = superblockFile;
		this.persister = persister;

		Pointer pointer = superblockFile.load(0);
		if (pointer == null)
			superblockFile.save(0, pointer = persister.save(new LazyIbTree<>((p0, p1) -> kc.compare(p0.t0, p1.t0))));
		store = new LazyIbTreeStore<>(persister.load(pointer));
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

	@Override
	public KeyValueStore<Key, Value> store() {
		return store;
	}

}
