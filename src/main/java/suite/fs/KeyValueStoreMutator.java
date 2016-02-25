package suite.fs;

public interface KeyValueStoreMutator<Key, Value> {

	public KeyValueStore<Key, Value> store();

	public void end(boolean isComplete);

}
