package suite.btree;

import suite.util.Pair;

public interface B_TreeInterface<Key, Value> {

	public void create();

	public Value get(Key key);

	public Iterable<Pair<Key, Value>> range(Key startKey, Key endKey);

	public void put(Key key, Value value);

	public void remove(Key key);

}
