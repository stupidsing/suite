package suite.btree;

import java.io.PrintStream;

import suite.util.Pair;

public interface B_Tree<Key, Value> {

	public void create();

	public void dump(PrintStream w);

	public Value get(Key key);

	public Iterable<Pair<Key, Value>> range(Key startKey, Key endKey);

	public void put(Key key, Value value);

	public void remove(Key key);

}
