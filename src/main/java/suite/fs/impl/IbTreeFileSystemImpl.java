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
	private FileSystemMutator mutator;

	public IbTreeFileSystemImpl(IbTreeConfiguration<Bytes> config) {
		config.setComparator(Bytes.comparator);
		config.setSerializer(keyUtil.serializer());
		ibTreeStack = new IbTreeStack<>(config);
		mutator = new FileSystemMutatorImpl(keyUtil, ibTreeStack.getIbTree()::begin);
	}

	@Override
	public void close() throws IOException {
		ibTreeStack.close();
	}

	@Override
	public void create() {
		ibTreeStack.getIbTree().create().end(true);
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
