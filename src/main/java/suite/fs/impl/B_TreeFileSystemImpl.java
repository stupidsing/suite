package suite.fs.impl;

import java.io.IOException;
import java.nio.file.Path;

import primal.primitive.adt.Bytes;
import suite.btree.B_Tree;
import suite.btree.impl.B_TreeBuilder;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;

public class B_TreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();
	private B_Tree<Bytes, Integer> b_tree;
	private FileSystemMutator mutator;

	public B_TreeFileSystemImpl(Path path, boolean isNew, int pageSize) {
		var serializer = keyUtil.serializer();
		var cmp = Bytes.comparator;
		var pair = B_TreeBuilder.build(isNew, path, pageSize, pageSize, serializer, cmp);

		b_tree = pair.k;
		var b_treeMutator = pair.v;
		mutator = new FileSystemMutatorImpl(keyUtil, () -> b_treeMutator);
	}

	@Override
	public void close() throws IOException {
		b_tree.close();
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
