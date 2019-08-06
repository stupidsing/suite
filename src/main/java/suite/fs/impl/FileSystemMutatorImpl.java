package suite.fs.impl;

import static java.lang.Math.min;

import java.util.List;

import primal.Verbs.Equals;
import primal.fp.Funs.Source;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import suite.file.PageFile;
import suite.fs.FileSystemMutator;
import suite.fs.KeyDataStore;

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
		var store = mutate.g();
		var kvm = store.mutate();
		var kdm = store.mutateData();

		var hash = keyUtil.hash(name);
		var size = kvm.get(key(hash, SIZEID, 0));

		if (size != null) {
			var seq = 0;
			var bb = new BytesBuilder();
			for (var s = 0; s < size; s += pageSize)
				bb.append(kdm.getPayload(key(hash, DATAID, seq++)));
			return bb.toBytes().range(0, size);
		} else
			return null;
	}

	public List<Bytes> list(Bytes start, Bytes end) {
		var store = mutate.g();
		return new FileSystemKeySet(keyUtil, store).list(start, end).toList();
	}

	public void replace(Bytes name, Bytes bytes) {
		var store = mutate.g();
		var kvm = store.mutate();
		var kdm = store.mutateData();

		var fsNameKeySet = new FileSystemKeySet(keyUtil, store);
		var hash = keyUtil.hash(name);
		var sizeKey = key(hash, SIZEID, 0);

		var nameBytes0 = fsNameKeySet.list(name, null).first();
		var isRemove = Equals.ab(nameBytes0, name);
		var isCreate = bytes != null;

		if (isRemove) {
			int seq = 0, size = kvm.get(sizeKey);

			if (!isCreate)
				fsNameKeySet.remove(name);
			kvm.remove(sizeKey);
			for (var s = 0; s < size; s += pageSize)
				kdm.removePayload(key(hash, DATAID, seq++));
		}

		if (isCreate) {
			int pos = 0, seq = 0, size = bytes.size();

			while (pos < size) {
				var pos1 = min(pos + pageSize, size);
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
		var store = mutate.g();
		var mutator = store.mutateData();

		mutator.putPayload(key(keyUtil.hash(name), DATAID, seq), bytes);
		store.end(true);
	}

	public void resize(Bytes name, int size1) {
		var store = mutate.g();
		var kvm = store.mutate();
		var kdm = store.mutateData();

		var hash = keyUtil.hash(name);
		var sizeKey = key(hash, SIZEID, 0);
		var size0 = kvm.get(sizeKey);
		var nPages0 = (size0 + pageSize - 1) / pageSize;
		var nPages1 = (size1 + pageSize - 1) / pageSize;

		for (var page = nPages1; page < nPages0; page++)
			kdm.removePayload(key(hash, DATAID, page));
		for (var page = nPages0; page < nPages1; page++)
			kdm.putPayload(key(hash, DATAID, page), Bytes.empty);

		kvm.put(sizeKey, size1);
		store.end(true);
	}

	private Bytes key(Bytes hash, int id, int seq) {
		return keyUtil.toBytes(keyUtil.toDataKey(hash, id, seq));
	}

}
