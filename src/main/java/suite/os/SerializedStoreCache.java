package suite.os;

import static suite.util.Friends.rethrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.serialize.SerInput;
import suite.serialize.SerOutput;
import suite.serialize.Serialize.Serializer;
import suite.streamlet.FunUtil.Source;
import suite.util.Fail;

public class SerializedStoreCache<K, V> {

	private Serializer<K> keySerializer;
	private Serializer<V> valueSerializer;
	private StoreCache storeCache = Singleton.me.storeCache;

	public static <V> SerializedStoreCache<String, V> of(Serializer<V> valueSerializer) {
		return of(Singleton.me.serialize.variableLengthString, valueSerializer);
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

		return rethrow(() -> {
			try (var bais = new ByteArrayInputStream(valueBytes.toArray()); var dis = SerInput.of(bais)) {
				return valueSerializer.read(dis);
			}
		});
	}

	private static <T> Bytes serialize(Serializer<T> serializer, T t) {
		var baos = new ByteArrayOutputStream();

		try (var so = SerOutput.of(baos)) {
			serializer.write(so, t);
		} catch (IOException ex) {
			Fail.t(ex);
		}

		return Bytes.of(baos.toByteArray());
	}

}
