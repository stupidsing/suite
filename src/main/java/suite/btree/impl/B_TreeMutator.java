package suite.btree.impl;

import java.util.Iterator;

import suite.btree.B_Tree;
import suite.immutable.btree.KeyDataStoreMutator;
import suite.primitive.Bytes;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;

public class B_TreeMutator<Key> implements KeyDataStoreMutator<Key> {

	private B_Tree<Key, Integer> b_tree;
	private Sink<Void> committer;

	public B_TreeMutator(B_Tree<Key, Integer> b_tree, Sink<Void> committer) {
		this.b_tree = b_tree;
		this.committer = committer;
	}

	@Override
	public void commit() {
		committer.sink(null);
	}

	@Override
	public Source<Key> keys() {
		throw new UnsupportedOperationException("No full traversal");
	}

	@Override
	public Source<Key> keys(Key start, Key end) {
		Iterator<Pair<Key, Integer>> iter = b_tree.range(start, end).iterator();
		return () -> iter.hasNext() ? iter.next().t0 : null;
	}

	@Override
	public Integer getData(Key key) {
		return b_tree.get(key);
	}

	@Override
	public Bytes getPayload(Key key) {
		return b_tree.getPayload(key);
	}

	@Override
	public void put(Key key) {
		b_tree.put(key);
	}

	@Override
	public void put(Key key, Integer data) {
		b_tree.put(key, data);
	}

	@Override
	public void put(Key key, Bytes payload) {
		b_tree.putPayload(key, payload);

	}

	@Override
	public void remove(Key key) {
		b_tree.remove(key);
	}

}
