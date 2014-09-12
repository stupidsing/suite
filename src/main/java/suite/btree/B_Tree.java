package suite.btree;

import java.io.Closeable;
import java.io.PrintStream;

import suite.primitive.Bytes;
import suite.util.Pair;

public interface B_Tree<Key, Value> extends Closeable {

	public void create();

	public void dump(PrintStream w);

	public Value get(Key key);

	public Bytes getPayload(Key key);

	public Iterable<Pair<Key, Value>> range(Key startKey, Key endKey);

	public void put(Key key, Value value);

	public void putPayload(Key key, Bytes payload);

	public void remove(Key key);

}
