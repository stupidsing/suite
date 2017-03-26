package suite.os;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import suite.primitive.Bytes;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
import suite.util.Serialize.Serializer;

public class SerializedStoreCache<K, V> {

	private Serializer<K> keySerializer;
	private Serializer<V> valueSerializer;
	private StoreCache storeCache = new StoreCache();

	public SerializedStoreCache(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
	}

	public V get(K key, Source<V> source) {
		return Rethrow.ioException(() -> {
			Bytes keyBytes = serialize(keySerializer, key);
			Bytes valueBytes = storeCache.get(keyBytes, () -> serialize(valueSerializer, source.source()));

			try (ByteArrayInputStream bais = new ByteArrayInputStream(valueBytes.toBytes());
					DataInputStream dis = new DataInputStream(bais)) {
				return valueSerializer.read(dis);
			}
		});
	}

	private static <T> Bytes serialize(Serializer<T> serializer, T t) {
		ByteArrayOutputStream baosKey = new ByteArrayOutputStream();

		try (DataOutputStream dos = new DataOutputStream(baosKey)) {
			serializer.write(dos, t);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return Bytes.of(baosKey.toByteArray());
	}

}
