package suite.fs.impl;

import java.io.IOException;

import suite.btree.B_Tree;
import suite.btree.impl.B_TreeBuilder;
import suite.btree.impl.B_TreeMutator;
import suite.file.impl.JournalledPageFileImpl;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.primitive.Bytes;
import suite.util.SerializeUtil;

public class B_TreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();
	private JournalledPageFileImpl jpf;
	private B_Tree<Bytes, Integer> b_tree;
	private FileSystemMutator mutator;

	public B_TreeFileSystemImpl(String filename, int pageSize) throws IOException {
		jpf = new JournalledPageFileImpl(filename, pageSize);

		b_tree = new B_TreeBuilder<>(keyUtil.serializer(), SerializeUtil.intSerializer) //
				.build(jpf, false, Bytes.comparator, pageSize);

		B_TreeMutator<Bytes> b_treeMutator = new B_TreeMutator<Bytes>(b_tree, () -> {
			try {
				jpf.commit();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});

		mutator = new FileSystemMutatorImpl(keyUtil, () -> b_treeMutator);
	}

	@Override
	public void close() throws IOException {
		b_tree.close();
	}

	@Override
	public void create() throws IOException {
		b_tree.create();
		jpf.commit();
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
