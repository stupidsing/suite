package suite.btree;

import java.io.Closeable;
import java.io.PrintStream;

import suite.adt.Pair;
import suite.fs.KeyValueStore;
import suite.primitive.Bytes;
import suite.streamlet.Streamlet;

public interface B_Tree<Key, Value> extends Closeable, KeyValueStore<Key, Value> {

	public void create();

	public void dump(PrintStream w);

	public Bytes getPayload(Key key);

	public boolean getTerminal(Key key);

	public Streamlet<Pair<Key, Value>> range(Key start, Key end);

	public void putPayload(Key key, Bytes payload);

	public void putTerminal(Key key);

}
