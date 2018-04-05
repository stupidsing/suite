package suite.fs.impl;

import java.io.IOException;
import java.nio.file.Path;

import suite.btree.B_Tree;
import suite.btree.impl.B_TreeBuilder;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.primitive.Bytes;

public class B_TreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();
	private B_Tree<Bytes, Integer> b_tree;
	private FileSystemMutator mutator;

	public B_TreeFileSystemImpl(Path path, boolean isNew, int pageSize) {
		var pair = B_TreeBuilder.build( //
				path, //
				isNew, //
				keyUtil.serializer(), //
				Bytes.comparator, //
				pageSize, //
				pageSize);
		b_tree = pair.t0;
		var b_treeMutator = pair.t1;
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
