package suite.fs;

import suite.streamlet.Streamlet;

public interface KeyValueStoreMutator<Key, Value> {

	public void commit();

	public Streamlet<Key> keys();

	public Streamlet<Key> keys(Key start, Key end);

	public Value get(Key key);

	/**
	 * Replaces a value by another without payload. For dictionary cases to
	 * replace stored value of the same key.
	 */
	public void put(Key key, Value data);

	public void remove(Key key);

}
