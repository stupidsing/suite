package suite.immutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import suite.adt.Pair;
import suite.file.SerializedPageFile;
import suite.fs.KeyValueStore;
import suite.fs.KeyValueStoreMutator;

public class LazyIbTreeMutator<Pointer, Key, Value> implements KeyValueStoreMutator<Key, Value> {

	private SerializedPageFile<Pointer> superblockFile;
	private LazyIbTreePersister<Pointer, Pair<Key, Value>> persister;
	private LazyIbTreeStore<Key, Value> store;

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
