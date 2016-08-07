package suite.fs;

public interface KeyDataStore<Key> extends KeyValueStore<Key, Integer> {

	public KeyDataMutator<Key> mutateData();

}
