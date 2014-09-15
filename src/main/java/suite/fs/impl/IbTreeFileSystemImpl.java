package suite.fs.impl;

import java.io.IOException;

import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.immutable.btree.impl.IbTreeConfiguration;
import suite.immutable.btree.impl.IbTreeStack;
import suite.primitive.Bytes;

public class IbTreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private IbTreeStack<Bytes> ibTreeStack;
	private FileSystemMutatorImpl mutator;

	public IbTreeFileSystemImpl(IbTreeConfiguration<Bytes> config) throws IOException {
		config.setComparator(Bytes.comparator);
		config.setSerializer(keyUtil.serializer());
		ibTreeStack = new IbTreeStack<>(config);
		mutator = new FileSystemMutatorImpl(keyUtil, ibTreeStack.getIbTree()::begin);
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
	public FileSystemMutator mutate() {
		return mutator;
	}

}
