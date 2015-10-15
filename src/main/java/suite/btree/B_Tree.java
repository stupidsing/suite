package suite.btree;

import java.io.Closeable;
import java.io.PrintStream;

import suite.adt.Pair;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueStore;
import suite.streamlet.Streamlet;

public interface B_Tree<Key, Value> extends Closeable, KeyDataStore<Key>, KeyValueStore<Key, Value> {

	public void create();

	public void dump(PrintStream w);

	public Streamlet<Pair<Key, Value>> range(Key start, Key end);

}
