package suite.fs;

import suite.util.FunUtil.Source;

public interface KeyValueStoreMutator<Key, Value> {

	public void commit();

	public Source<Key> keys();

	public Source<Key> keys(Key start, Key end);

	public Value getData(Key key);

	/**
	 * Replaces a value with another without payload. For dictionary cases to
	 * replace stored value of the same key.
	 */
	public void put(Key key, Value data);

	public void remove(Key key);

}
