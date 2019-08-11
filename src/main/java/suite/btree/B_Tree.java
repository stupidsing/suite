package suite.btree;

import java.io.Closeable;
import java.io.PrintStream;

import primal.adt.Pair;
import primal.streamlet.Streamlet;
import suite.fs.KeyDataMutator;
import suite.fs.KeyValueMutator;

public interface B_Tree<Key, Value> extends Closeable, KeyDataMutator<Key>, KeyValueMutator<Key, Value> {

	public void create();

	public void dump(PrintStream w);

	public Streamlet<Pair<Key, Value>> range(Key start, Key end);

}
