package suite.fs.impl;

import static suite.util.Friends.min;

import java.util.List;
import java.util.Objects;

import suite.file.PageFile;
import suite.fs.FileSystemMutator;
import suite.fs.KeyDataMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueMutator;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;

public class FileSystemMutatorImpl implements FileSystemMutator {

	private byte DATAID = 64;
	private byte SIZEID = 65;
	private int pageSize = PageFile.defaultPageSize;

	private FileSystemKeyUtil keyUtil;
	private Source<KeyDataStore<Bytes>> mutate;

	public FileSystemMutatorImpl(FileSystemKeyUtil keyUtil, Source<KeyDataStore<Bytes>> mutate) {
		this.keyUtil = keyUtil;
		this.mutate = mutate;
	}

	public Bytes read(Bytes name) {
		KeyDataStore<Bytes> store = mutate.source();
		KeyValueMutator<Bytes, Integer> kvm = store.mutate();
		KeyDataMutator<Bytes> kdm = store.mutateData();

		Bytes hash = keyUtil.hash(name);
		Integer size = kvm.get(key(hash, SIZEID, 0));

		if (size != null) {
			var seq = 0;
			BytesBuilder bb = new BytesBuilder();
			for (int s = 0; s < size; s += pageSize)
				bb.append(kdm.getPayload(key(hash, DATAID, seq++)));
			return bb.toBytes().range(0, size);
		} else
			return null;
	}

	public List<Bytes> list(Bytes start, Bytes end) {
		KeyDataStore<Bytes> store = mutate.source();
		return new FileSystemKeySet(keyUtil, store).list(start, end).toList();
	}

	public void replace(Bytes name, Bytes bytes) {
		KeyDataStore<Bytes> store = mutate.source();
		KeyValueMutator<Bytes, Integer> kvm = store.mutate();
		KeyDataMutator<Bytes> kdm = store.mutateData();

		FileSystemKeySet fsNameKeySet = new FileSystemKeySet(keyUtil, store);
		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);

		Bytes nameBytes0 = fsNameKeySet.list(name, null).first();
		boolean isRemove = Objects.equals(nameBytes0, name);
		boolean isCreate = bytes != null;

		if (isRemove) {
			int seq = 0, size = kvm.get(sizeKey);

			if (!isCreate)
				fsNameKeySet.remove(name);
			kvm.remove(sizeKey);
			for (int s = 0; s < size; s += pageSize)
				kdm.removePayload(key(hash, DATAID, seq++));
		}

		if (isCreate) {
			int pos = 0, seq = 0, size = bytes.size();

			while (pos < size) {
				int pos1 = min(pos + pageSize, size);
				kdm.putPayload(key(hash, DATAID, seq++), bytes.range(pos, pos1));
				pos = pos1;
			}
			kvm.put(sizeKey, size);
			if (!isRemove)
				fsNameKeySet.add(name);
		}

		store.end(true);
	}

	public void replace(Bytes name, int seq, Bytes bytes) {
		KeyDataStore<Bytes> store = mutate.source();
		KeyDataMutator<Bytes> mutator = store.mutateData();

		mutator.putPayload(key(keyUtil.hash(name), DATAID, seq), bytes);
		store.end(true);
	}

	public void resize(Bytes name, int size1) {
		KeyDataStore<Bytes> store = mutate.source();
		KeyValueMutator<Bytes, Integer> kvm = store.mutate();
		KeyDataMutator<Bytes> kdm = store.mutateData();

		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);
		var size0 = kvm.get(sizeKey);
		var nPages0 = (size0 + pageSize - 1) / pageSize;
		var nPages1 = (size1 + pageSize - 1) / pageSize;

		for (int page = nPages1; page < nPages0; page++)
			kdm.removePayload(key(hash, DATAID, page));
		for (int page = nPages0; page < nPages1; page++)
			kdm.putPayload(key(hash, DATAID, page), Bytes.empty);

		kvm.put(sizeKey, size1);
		store.end(true);
	}

	private Bytes key(Bytes hash, int id, int seq) {
		return keyUtil.toBytes(keyUtil.toDataKey(hash, id, seq));
	}

}
