package suite.fs.impl;

import java.util.List;
import java.util.Objects;

import suite.file.PageFile;
import suite.fs.FileSystemMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyDataStoreMutator;
import suite.fs.KeyValueStore;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;

public class FileSystemMutatorImpl implements FileSystemMutator {

	private byte DATAID = 64;
	private byte SIZEID = 65;
	private int pageSize = PageFile.defaultPageSize;

	private FileSystemKeyUtil keyUtil;
	private Source<KeyDataStoreMutator<Bytes>> mutate;

	public FileSystemMutatorImpl(FileSystemKeyUtil keyUtil, Source<KeyDataStoreMutator<Bytes>> mutate) {
		this.keyUtil = keyUtil;
		this.mutate = mutate;
	}

	public Bytes read(Bytes name) {
		KeyDataStoreMutator<Bytes> mutator = mutate.source();
		KeyValueStore<Bytes, Integer> store = mutator.store();
		KeyDataStore<Bytes> dataStore = mutator.dataStore();

		Bytes hash = keyUtil.hash(name);
		Integer size = store.get(key(hash, SIZEID, 0));

		if (size != null) {
			int seq = 0;
			BytesBuilder bb = new BytesBuilder();
			for (int s = 0; s < size; s += pageSize)
				bb.append(dataStore.getPayload(key(hash, DATAID, seq++)));
			return bb.toBytes().subbytes(0, size);
		} else
			return null;
	}

	public List<Bytes> list(Bytes start, Bytes end) {
		KeyDataStoreMutator<Bytes> mutator = mutate.source();
		return new FileSystemKeySet(keyUtil, mutator).list(start, end).toList();
	}

	public void replace(Bytes name, Bytes bytes) {
		KeyDataStoreMutator<Bytes> mutator = mutate.source();
		KeyValueStore<Bytes, Integer> store = mutator.store();
		KeyDataStore<Bytes> dataStore = mutator.dataStore();

		FileSystemKeySet fsNameKeySet = new FileSystemKeySet(keyUtil, mutator);
		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);

		Bytes nameBytes0 = fsNameKeySet.list(name, null).first();
		boolean isRemove = Objects.equals(nameBytes0, name);
		boolean isCreate = bytes != null;

		if (isRemove) {
			int seq = 0, size = store.get(sizeKey);

			if (!isCreate)
				fsNameKeySet.remove(name);
			store.remove(sizeKey);
			for (int s = 0; s < size; s += pageSize)
				dataStore.removePayload(key(hash, DATAID, seq++));
		}

		if (isCreate) {
			int pos = 0, seq = 0, size = bytes.size();

			while (pos < size) {
				int pos1 = Math.min(pos + pageSize, size);
				dataStore.putPayload(key(hash, DATAID, seq++), bytes.subbytes(pos, pos1));
				pos = pos1;
			}
			store.put(sizeKey, size);
			if (!isRemove)
				fsNameKeySet.add(name);
		}

		mutator.end(true);
	}

	public void replace(Bytes name, int seq, Bytes bytes) {
		KeyDataStoreMutator<Bytes> mutator = mutate.source();
		KeyDataStore<Bytes> dataStore = mutator.dataStore();

		dataStore.putPayload(key(keyUtil.hash(name), DATAID, seq), bytes);
		mutator.end(true);
	}

	public void resize(Bytes name, int size1) {
		KeyDataStoreMutator<Bytes> mutator = mutate.source();
		KeyValueStore<Bytes, Integer> store = mutator.store();
		KeyDataStore<Bytes> dataStore = mutator.dataStore();

		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);
		int size0 = store.get(sizeKey);
		int nPages0 = (size0 + pageSize - 1) / pageSize;
		int nPages1 = (size1 + pageSize - 1) / pageSize;

		for (int page = nPages1; page < nPages0; page++)
			dataStore.removePayload(key(hash, DATAID, page));
		for (int page = nPages0; page < nPages1; page++)
			dataStore.putPayload(key(hash, DATAID, page), Bytes.empty);

		store.put(sizeKey, size1);
		mutator.end(true);
	}

	private Bytes key(Bytes hash, int id, int seq) {
		return keyUtil.toBytes(keyUtil.toDataKey(hash, id, seq));
	}

}
