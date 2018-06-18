package suite.os;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.util.SerInput;
import suite.util.SerOutput;
import suite.util.Fail;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class SerializedStoreCache<K, V> {

	private Serializer<K> keySerializer;
	private Serializer<V> valueSerializer;
	private StoreCache storeCache = Singleton.me.storeCache;

	public static <V> SerializedStoreCache<String, V> of(Serializer<V> valueSerializer) {
		return of(Serialize.me.variableLengthString, valueSerializer);
	}

	public static <K, V> SerializedStoreCache<K, V> of(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		return new SerializedStoreCache<>(keySerializer, valueSerializer);
	}

	private SerializedStoreCache(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
	}

	public V get(K key, Source<V> source) {
		var keyBytes = serialize(keySerializer, key);
		var valueBytes = storeCache.get(keyBytes, () -> serialize(valueSerializer, source.source()));

		return Rethrow.ex(() -> {
			try (var bais = new ByteArrayInputStream(valueBytes.toArray()); var dis = SerInput.of(bais)) {
				return valueSerializer.read(dis);
			}
		});
	}

	private static <T> Bytes serialize(Serializer<T> serializer, T t) {
		var baosKey = new ByteArrayOutputStream();

		try (var dos = SerOutput.of(baosKey)) {
			serializer.write(dos, t);
		} catch (IOException ex) {
			Fail.t(ex);
		}

		return Bytes.of(baosKey.toByteArray());
	}

}
