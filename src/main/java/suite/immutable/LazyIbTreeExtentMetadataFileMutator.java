package suite.immutable;

import java.util.Comparator;

import suite.adt.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreeExtentMetadataFileMutator<K, V> extends LazyIbTreeMutator<Extent, K, V> {

	public static <K, V> LazyIbTreeExtentMetadataFileMutator<K, V> of( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		Comparator<Pair<K, V>> pc = (p0, p1) -> kc.compare(p0.t0, p1.t0);
		Serializer<Pair<K, V>> ps = Serialize.pair(ks, vs);
		PageFile pf0 = new SubPageFileImpl(pageFile, 0, 1);
		PageFile pf1 = new SubPageFileImpl(pageFile, 1, Integer.MAX_VALUE);
		SerializedPageFile<Extent> superblockFile = new SerializedPageFileImpl<>(pf0, Serialize.nullable(Serialize.extent()));
		LazyIbTreePersister<Extent, Pair<K, V>> persister = new LazyIbTreeExtentMetadataFilePersister<>(pf1, pc, ps);
		return new LazyIbTreeExtentMetadataFileMutator<>(superblockFile, persister, kc);
	}

	private LazyIbTreeExtentMetadataFileMutator( //
			SerializedPageFile<Extent> superblockFile, //
			LazyIbTreePersister<Extent, Pair<K, V>> persister, //
			Comparator<K> kc) {
		super(superblockFile, persister, kc);
	}

}
