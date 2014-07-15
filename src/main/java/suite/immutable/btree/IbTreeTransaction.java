package suite.immutable.btree;

import suite.primitive.Bytes;
import suite.util.FunUtil.Source;

public interface IbTreeTransaction<Key> {

	public void commit();

	public Source<Key> keys();

	public Source<Key> keys(Key start, Key end);

	public Integer getData(Key key);

	public Bytes getPayload(Key key);

	/**
	 * Replaces a value with another without payload. For dictionary cases to
	 * replace stored value of the same key.
	 */
	public void put(Key key);

	public void put(Key key, Integer data);

	/**
	 * Replaces a value with another, attached with a payload of page data. For
	 * dictionary cases to replace stored value of the same key.
	 *
	 * Asserts comparator.compare(<original-key>, key) == 0.
	 */
	public <Payload> void put(Key key, Bytes payload);

	public void remove(Key key);

}
