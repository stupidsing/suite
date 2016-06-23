package suite.immutable;

import java.util.Comparator;

import suite.adt.Pair;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.PageFileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreePageFileMutator<K, V> extends LazyIbTreeMutator<Integer, K, V> {

	public static <K, V> LazyIbTreePageFileMutator<K, V> of( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		Comparator<Pair<K, V>> pc = (p0, p1) -> kc.compare(p0.t0, p1.t0);
		Serializer<Pair<K, V>> ps = Serialize.pair(ks, vs);
		PageFile pf0 = PageFileFactory.subPageFile(pageFile, 0, 1);
		PageFile pf1 = PageFileFactory.subPageFile(pageFile, 1, Integer.MAX_VALUE);
		SerializedPageFile<Integer> superblockFile = SerializedFileFactory.serialized(pf0, Serialize.nullable(Serialize.int_));
		LazyIbTreePersister<Integer, Pair<K, V>> persister = new LazyIbTreePageFilePersister<>(pf1, pc, ps);
		return new LazyIbTreePageFileMutator<>(superblockFile, persister, kc);
	}

	private LazyIbTreePageFileMutator( //
			SerializedPageFile<Integer> superblockFile, //
			LazyIbTreePersister<Integer, Pair<K, V>> persister, //
			Comparator<K> kc) {
		super(superblockFile, persister, kc);
	}

}
