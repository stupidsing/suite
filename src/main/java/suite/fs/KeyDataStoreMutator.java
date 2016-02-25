package suite.fs;

public interface KeyDataStoreMutator<Key> extends KeyValueStoreMutator<Key, Integer> {

	public KeyDataStore<Key> dataStore();

}
