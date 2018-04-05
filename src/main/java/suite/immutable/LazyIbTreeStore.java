package suite.immutable;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import suite.adt.pair.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.fs.KeyValueMutator;
import suite.fs.KeyValueStore;
import suite.util.Fail;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreeStore<Pointer, Key, Value> implements KeyValueStore<Key, Value> {

	private static Serialize serialize = Serialize.me;

	private SerializedPageFile<Pointer> superblockFile;
	private LazyIbTreePersister<Pointer, Pair<Key, Value>> persister;
	private LazyIbTreeMutator<Key, Value> mutator;

	public static <K, V> LazyIbTreeStore<Extent, K, V> ofExtent( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		Comparator<Pair<K, V>> pc = (p0, p1) -> {
			boolean b0 = p0 != null;
			boolean b1 = p1 != null;
			if (b0 && b1)
				return kc.compare(p0.t0, p1.t0);
			else
				return b0 ? 1 : b1 ? -1 : 0;
		};
		Serializer<Pair<K, V>> ps = serialize.pair(ks, vs);
		Serializer<Extent> xs = serialize.nullable(serialize.extent());
		PageFile[] pfs = FileFactory.subPageFiles(pageFile, 0, 1, Integer.MAX_VALUE);
		SerializedPageFile<Extent> superblockFile = SerializedFileFactory.serialized(pfs[0], xs);
		LazyIbTreePersister<Extent, Pair<K, V>> persister = new LazyIbTreeExtentFilePersister<>(pfs[1], pc, ps);
		return new LazyIbTreeStore<>(superblockFile, persister, kc);
	}

	public static <K, V> LazyIbTreeStore<Integer, K, V> ofPage( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		Comparator<Pair<K, V>> pc = (p0, p1) -> kc.compare(p0.t0, p1.t0);
		Serializer<Pair<K, V>> ps = serialize.pair(ks, vs);
		PageFile[] pfs = FileFactory.subPageFiles(pageFile, 0, 1, Integer.MAX_VALUE);
		SerializedPageFile<Integer> superblockFile = SerializedFileFactory.serialized(pfs[0], serialize.nullable(serialize.int_));
		LazyIbTreePersister<Integer, Pair<K, V>> persister = new LazyIbTreePageFilePersister<>(pfs[1], pc, ps);
		return new LazyIbTreeStore<>(superblockFile, persister, kc);
	}

	public LazyIbTreeStore( //
			SerializedPageFile<Pointer> superblockFile, //
			LazyIbTreePersister<Pointer, Pair<Key, Value>> persister, //
			Comparator<Key> kc) {
		this.superblockFile = superblockFile;
		this.persister = persister;

		var pointer = superblockFile.load(0);
		if (pointer == null)
			superblockFile.save(0, pointer = persister.save(new LazyIbTree<>((p0, p1) -> kc.compare(p0.t0, p1.t0))));
		mutator = new LazyIbTreeMutator<>(persister.load(pointer));
	}

	@Override
	public synchronized void end(boolean isComplete) {
		if (isComplete) {
			var pointer1 = persister.save(mutator.get());
			Pointer pointerx = persister.gc(List.of(pointer1), 9).getOrDefault(pointer1, pointer1);
			superblockFile.save(0, pointerx);
		}

		try {
			persister.close();
			superblockFile.close();
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	@Override
	public KeyValueMutator<Key, Value> mutate() {
		return mutator;
	}

}
