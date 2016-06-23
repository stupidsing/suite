package suite.fs.impl;

import java.io.IOException;
import java.nio.file.Path;

import suite.btree.B_Tree;
import suite.btree.impl.B_TreeBuilder;
import suite.btree.impl.B_TreeMutator;
import suite.file.JournalledPageFile;
import suite.file.impl.JournalledFileFactory;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.fs.KeyDataStoreMutator;
import suite.primitive.Bytes;
import suite.util.Serialize;

public class B_TreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();
	private JournalledPageFile jpf;
	private B_Tree<Bytes, Integer> b_tree;
	private FileSystemMutator mutator;

	public B_TreeFileSystemImpl(Path path, int pageSize) {
		jpf = JournalledFileFactory.journalled(path, pageSize);
		b_tree = new B_TreeBuilder<>(keyUtil.serializer(), Serialize.int_).build(jpf, false, Bytes.comparator, pageSize);

		KeyDataStoreMutator<Bytes> b_treeMutator = new B_TreeMutator<>(b_tree, () -> jpf.commit());

		mutator = new FileSystemMutatorImpl(keyUtil, () -> b_treeMutator);
	}

	@Override
	public void close() throws IOException {
		b_tree.close();
	}

	@Override
	public void create() {
		b_tree.create();
		jpf.commit();
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
