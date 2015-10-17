package suite.fs;

public interface KeyValueStoreMutator<Key, Value> extends KeyValueStore<Key, Value> {

	public void end(boolean isComplete);

}
