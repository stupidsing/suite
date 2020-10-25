package suite.object;

import java.util.Map;
import java.util.WeakHashMap;

public interface MetadataDefaults<T> {

	public static Map<Object, Object> metadata = new WeakHashMap<>();

	public default T getMetadata() {
		@SuppressWarnings("unchecked")
		var t = (T) metadata.get(this);
		return t;
	}

	public default T setMetadata(T t) {
		@SuppressWarnings("unchecked")
		T t0 = (T) metadata.put(this, t);
		return t0;
	}

}
