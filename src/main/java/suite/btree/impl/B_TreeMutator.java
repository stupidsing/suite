package suite.btree.impl;

import suite.btree.B_Tree;
import suite.fs.KeyDataStore;
import suite.fs.KeyDataStoreMutator;
import suite.fs.KeyValueStore;

public class B_TreeMutator<Key> implements KeyDataStoreMutator<Key> {

	private B_Tree<Key, Integer> b_tree;
	private Runnable committer;

	public B_TreeMutator(B_Tree<Key, Integer> b_tree, Runnable committer) {
		this.b_tree = b_tree;
		this.committer = committer;
	}

	@Override
	public void end(boolean isComplete) {
		if (isComplete)
			committer.run();
	}

	@Override
	public KeyValueStore<Key, Integer> store() {
		return b_tree;
	}

	@Override
	public KeyDataStore<Key> dataStore() {
		return b_tree;
	}

}
