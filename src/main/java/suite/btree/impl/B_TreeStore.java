package suite.btree.impl;

import suite.btree.B_Tree;
import suite.fs.KeyDataMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueMutator;

public class B_TreeStore<Key> implements KeyDataStore<Key> {

	private B_Tree<Key, Integer> b_tree;
	private Runnable committer;

	public B_TreeStore(B_Tree<Key, Integer> b_tree, Runnable committer) {
		this.b_tree = b_tree;
		this.committer = committer;
	}

	@Override
	public void end(boolean isComplete) {
		if (isComplete)
			committer.run();
	}

	@Override
	public KeyValueMutator<Key, Integer> mutate() {
		return b_tree;
	}

	@Override
	public KeyDataMutator<Key> mutateData() {
		return b_tree;
	}

}
