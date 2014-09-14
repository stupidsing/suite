package suite.immutable.btree.impl;

import java.io.IOException;
import java.util.List;

import suite.immutable.btree.FileSystem;
import suite.immutable.btree.KeyDataStoreMutator;
import suite.primitive.Bytes;

public class FileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private IbTreeStack<Bytes> ibTreeStack;

	public FileSystemImpl(IbTreeConfiguration<Bytes> config) throws IOException {
		config.setComparator(Bytes.comparator);
		config.setSerializer(keyUtil.serializer());
		ibTreeStack = new IbTreeStack<>(config);
	}

	@Override
	public void close() {
		ibTreeStack.close();
	}

	@Override
	public void create() {
		ibTreeStack.getIbTree().create().commit();
	}

	@Override
	public Bytes read(Bytes name) {
		return new FileSystemMutator(mutator()).read(name);
	}

	@Override
	public List<Bytes> list(Bytes start, Bytes end) {
		return new FileSystemMutator(mutator()).list(start, end);
	}

	@Override
	public void replace(Bytes name, Bytes bytes) {
		new FileSystemMutator(mutator()).replace(name, bytes);
	}

	@Override
	public void replace(Bytes name, int seq, Bytes bytes) {
		new FileSystemMutator(mutator()).replace(name, seq, bytes);
	}

	@Override
	public void resize(Bytes name, int size1) {
		new FileSystemMutator(mutator()).resize(name, size1);
	}

	private KeyDataStoreMutator<Bytes> mutator() {
		return ibTreeStack.getIbTree().begin();
	}

}
