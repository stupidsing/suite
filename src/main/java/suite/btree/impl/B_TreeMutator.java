package suite.btree.impl;

import suite.btree.B_Tree;
import suite.fs.KeyDataStoreMutator;
import suite.primitive.Bytes;
import suite.streamlet.Streamlet;

public class B_TreeMutator<Key> implements KeyDataStoreMutator<Key> {

	private B_Tree<Key, Integer> b_tree;
	private Runnable committer;

	public B_TreeMutator(B_Tree<Key, Integer> b_tree, Runnable committer) {
		this.b_tree = b_tree;
		this.committer = committer;
	}

	@Override
	public void commit() {
		committer.run();
	}

	@Override
	public Streamlet<Key> keys() {
		throw new UnsupportedOperationException("No full traversal");
	}

	@Override
	public Streamlet<Key> keys(Key start, Key end) {
		return b_tree.keys(start, end);
	}

	@Override
	public Integer get(Key key) {
		return b_tree.get(key);
	}

	@Override
	public Bytes getPayload(Key key) {
		return b_tree.getPayload(key);
	}

	@Override
	public void putTerminal(Key key) {
		b_tree.put(key);
	}

	@Override
	public void put(Key key, Integer data) {
		b_tree.put(key, data);
	}

	@Override
	public void putPayload(Key key, Bytes payload) {
		b_tree.putPayload(key, payload);

	}

	@Override
	public void remove(Key key) {
		b_tree.remove(key);
	}

}
