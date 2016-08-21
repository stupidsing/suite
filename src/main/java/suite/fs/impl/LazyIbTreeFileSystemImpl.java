package suite.fs.impl;

import java.io.IOException;
import java.nio.file.Path;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.JournalledFileFactory;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.fs.KeyDataMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueMutator;
import suite.fs.KeyValueStore;
import suite.immutable.LazyIbTreeStore;
import suite.primitive.Bytes;
import suite.streamlet.Streamlet;
import suite.util.Serialize;

public class LazyIbTreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();
	private JournalledPageFile jpf;
	private FileSystemMutator mutator;

	public LazyIbTreeFileSystemImpl(Path path, int pageSize) {
		jpf = JournalledFileFactory.journalled(path, pageSize);
		PageFile pfs[] = FileFactory.subPageFiles(jpf, 0, 10240, 20480, 30720);

		mutator = new FileSystemMutatorImpl(keyUtil, () -> new KeyDataStore<Bytes>() {
			private KeyValueStore<Bytes, Bytes> kvss = LazyIbTreeStore.ofExtent( //
					pfs[0], //
					Bytes.comparator, //
					Serialize.variableLengthBytes, //
					Serialize.variableLengthBytes);

			private KeyValueStore<Bytes, Integer> kvsis = LazyIbTreeStore.ofExtent( //
					pfs[1], //
					Bytes.comparator, //
					Serialize.variableLengthBytes, //
					Serialize.int_);

			private KeyValueStore<Bytes, Boolean> kvsbs = LazyIbTreeStore.ofExtent( //
					pfs[2], //
					Bytes.comparator, //
					Serialize.variableLengthBytes, //
					Serialize.boolean_);

			public KeyValueMutator<Bytes, Integer> mutate() {
				return kvsis.mutate();
			}

			public KeyDataMutator<Bytes> mutateData() {
				return new KeyDataMutator<Bytes>() {
					public Streamlet<Bytes> keys(Bytes start, Bytes end) {
						return kvsbs.mutate().keys(start, end);
					}

					public Bytes getPayload(Bytes key) {
						return kvss.mutate().get(key);
					}

					public boolean getTerminal(Bytes key) {
						return kvsbs.mutate().get(key) == Boolean.TRUE;
					}

					public void putPayload(Bytes key, Bytes payload) {
						kvss.mutate().put(key, payload);
					}

					public void putTerminal(Bytes key) {
						kvsbs.mutate().put(key, Boolean.TRUE);
					}

					public void removePayload(Bytes key) {
						kvss.mutate().remove(key);
					}

					public void removeTerminal(Bytes key) {
						kvsbs.mutate().remove(key);
					}
				};
			}

			public void end(boolean isComplete) {
				kvss.end(isComplete);
				kvsis.end(isComplete);
				kvsbs.end(isComplete);
				jpf.commit();
			}
		});
	}

	@Override
	public void close() throws IOException {
		jpf.close();
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
