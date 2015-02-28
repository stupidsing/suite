package suite.btree;

import java.io.Closeable;
import java.io.PrintStream;

import suite.adt.Pair;
import suite.primitive.Bytes;
import suite.streamlet.Streamlet;

public interface B_Tree<Key, Value> extends Closeable {

	public void create();

	public void dump(PrintStream w);

	public Value get(Key key);

	public Bytes getPayload(Key key);

	public Streamlet<Key> keys(Key key0, Key key1);

	public Streamlet<Pair<Key, Value>> range(Key key0, Key key1);

	public void put(Key key);

	public void put(Key key, Value value);

	public void putPayload(Key key, Bytes payload);

	public void remove(Key key);

}
