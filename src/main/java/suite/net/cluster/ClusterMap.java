package suite.net.cluster;

public interface ClusterMap<K, V> {

	public V get(K key);

	public V set(K key, V value);

}
