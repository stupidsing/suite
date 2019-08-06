package suite.fs;

import primal.primitive.adt.Bytes;
import suite.streamlet.Streamlet;

public interface KeyDataMutator<Key> {

	public Streamlet<Key> keys(Key start, Key end);

	public Bytes getPayload(Key key);

	public boolean getTerminal(Key key);

	/**
	 * Replaces a value with another, attached with a payload of page data. For
	 * dictionary cases to replace stored value of the same key.
	 *
	 * Asserts comparator.compare(<original-key>, key) == 0.
	 */
	public void putPayload(Key key, Bytes payload);

	/**
	 * Puts a key only, without any value or payload.
	 */
	public void putTerminal(Key key);

	public void removePayload(Key key);

	public void removeTerminal(Key key);

}
