package suite.fs;

import suite.streamlet.Streamlet;

public interface KeyValueMutator<Key, Value> {

	public Streamlet<Key> keys(Key start, Key end);

	public Value get(Key key);

	/**
	 * Replaces a value by another without payload. For dictionary cases to
	 * replace stored value of the same key.
	 */
	public void put(Key key, Value value);

	public void remove(Key key);

}
