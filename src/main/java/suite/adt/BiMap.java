package suite.adt;

import java.util.Map;

public interface BiMap<K, V> extends Map<K, V> {

	public BiMap<V, K> inverse();

}
