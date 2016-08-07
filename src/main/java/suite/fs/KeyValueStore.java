package suite.fs;

public interface KeyValueStore<Key, Value> {

	public KeyValueMutator<Key, Value> mutate();

	public void end(boolean isComplete);

}
